package com.example.trinots.service;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.domain.extra.CodigoVerificacao;
import com.example.trinots.exception.exceptions.TokenInvalidoException;
import com.example.trinots.repository.CodigoVerificacaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Códigos de 6 dígitos (verificação de e-mail e recuperação de senha), válidos por 30 minutos,
 * uso único e com limite de tentativas. Apenas o hash do código é guardado.
 */
@Service
@Transactional
public class VerificacaoService {

    public static final int VALIDADE_MINUTOS = 30;
    public static final int MAX_TENTATIVAS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CodigoVerificacaoRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public VerificacaoService(CodigoVerificacaoRepository codigoRepository,
                              PasswordEncoder passwordEncoder,
                              EmailService emailService) {
        this.codigoRepository = codigoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Invalida os códigos anteriores ainda ativos do mesmo tipo, gera um novo e o envia por e-mail. */
    public void gerarEEnviar(Usuario usuario, TipoCodigoEnum tipo) {
        for (CodigoVerificacao anterior : codigoRepository.findByUsuarioIdUsuarioAndTipoAndUsadoFalse(usuario.getIdUsuario(), tipo)) {
            anterior.setUsado(true);
            codigoRepository.save(anterior);
        }

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

        CodigoVerificacao novo = new CodigoVerificacao();
        novo.setUsuario(usuario);
        novo.setTipo(tipo);
        novo.setCodigoHash(passwordEncoder.encode(codigo));
        novo.setDataExpiracao(Instant.now().plus(VALIDADE_MINUTOS, ChronoUnit.MINUTES));
        codigoRepository.save(novo);

        emailService.enviarCodigo(usuario, tipo, codigo, VALIDADE_MINUTOS);
    }

    /**
     * Valida o código e o marca como usado. Tentativas erradas são contabilizadas (por isso a transação
     * não deve ser desfeita quando a exceção sobe).
     */
    @Transactional(dontRollbackOn = TokenInvalidoException.class)
    public void validarEConsumir(Usuario usuario, TipoCodigoEnum tipo, String codigo) {
        CodigoVerificacao registro = codigoRepository
                .findFirstByUsuarioIdUsuarioAndTipoAndUsadoFalseOrderByDataExpiracaoDesc(usuario.getIdUsuario(), tipo)
                .orElseThrow(() -> new TokenInvalidoException("Código inválido ou expirado"));

        if (registro.getDataExpiracao().isBefore(Instant.now())) {
            registro.setUsado(true);
            codigoRepository.save(registro);
            throw new TokenInvalidoException("Código inválido ou expirado");
        }

        if (registro.getTentativas() >= MAX_TENTATIVAS) {
            throw new TokenInvalidoException("Muitas tentativas incorretas. Solicite um novo código");
        }

        if (!passwordEncoder.matches(codigo, registro.getCodigoHash())) {
            registro.setTentativas(registro.getTentativas() + 1);
            codigoRepository.save(registro);
            throw new TokenInvalidoException("Código inválido ou expirado");
        }

        registro.setUsado(true);
        codigoRepository.save(registro);
    }
}
