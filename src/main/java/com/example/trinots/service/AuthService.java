package com.example.trinots.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.domain.extra.TokenRevogado;
import com.example.trinots.dto.AuthDTO.*;
import com.example.trinots.exception.exceptions.*;
import com.example.trinots.repository.TokenRevogadoRepository;
import com.example.trinots.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Transactional
public class AuthService {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final VerificacaoService verificacaoService;
    private final TokenRevogadoRepository tokenRevogadoRepository;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokenService, VerificacaoService verificacaoService,
                       TokenRevogadoRepository tokenRevogadoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.verificacaoService = verificacaoService;
        this.tokenRevogadoRepository = tokenRevogadoRepository;
    }

    public LoginResponseDTO autenticar(LoginRequestDTO dto) {
        Usuario usuario = buscarPorCredenciais(dto);

        if (!usuario.isEmailVerificado()) {
            throw new EmailNaoVerificadoException("E-mail ainda não verificado. Informe o código enviado ou solicite um novo.");
        }

        if (!usuario.isAtivo()) {
            throw new ContaDesativadaException("Conta desativada. Reative a conta para continuar.");
        }

        String token = accessTokenService.generateToken(usuario);
        return new LoginResponseDTO("Bearer " + token, usuario.getIdUsuario(), usuario.getNome());
    }

    /** Invalida o token da sessão atual: ele não pode mais ser reutilizado. Idempotente. */
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIXO_BEARER)) {
            return;
        }

        DecodedJWT jwt = accessTokenService.decodificar(authorizationHeader.substring(PREFIXO_BEARER.length()));
        if (jwt == null || jwt.getId() == null || jwt.getExpiresAt() == null) {
            return;
        }

        tokenRevogadoRepository.save(new TokenRevogado(jwt.getId(), jwt.getExpiresAt().toInstant()));
    }

    public void solicitarRecuperacaoSenha(SolicitarRecuperacaoDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElse(null);

        if (usuario == null) {
            log.info("Solicitação de recuperação para email não cadastrado");
            return;
        }

        verificacaoService.gerarEEnviar(usuario, TipoCodigoEnum.RECUPERACAO_SENHA);
    }

    @Transactional(dontRollbackOn = TokenInvalidoException.class)
    public void redefinirSenhaComToken(RedefinirSenhaComTokenDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new TokenInvalidoException("Código inválido ou expirado"));

        verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, dto.codigo());

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);

        log.info("Senha redefinida com sucesso para o usuário {}", usuario.getIdUsuario());
    }

    @Transactional(dontRollbackOn = TokenInvalidoException.class)
    public void verificarEmail(VerificarEmailDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new TokenInvalidoException("Código inválido ou expirado"));

        verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, dto.codigo());

        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
    }

    public void reenviarVerificacao(ReenviarVerificacaoDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElse(null);

        if (usuario == null || usuario.isEmailVerificado()) {
            return;
        }

        verificacaoService.gerarEEnviar(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL);
    }

    /** Reativa uma conta desativada. Exige as credenciais, pois contas desativadas não conseguem autenticar. */
    public void reativarConta(LoginRequestDTO dto) {
        Usuario usuario = buscarPorCredenciais(dto);

        if (usuario.isAtivo()) {
            return;
        }

        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarPorCredenciais(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }
        return usuario;
    }
}
