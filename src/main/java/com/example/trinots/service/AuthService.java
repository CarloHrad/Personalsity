package com.example.trinots.service;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.extra.TokenRecuperacaoSenha;
import com.example.trinots.dto.AuthDTO.*;
import com.example.trinots.exception.exceptions.CredenciaisInvalidasException;
import com.example.trinots.exception.exceptions.TokenInvalidoException;
import com.example.trinots.repository.TokenRecuperacaoRepository;
import com.example.trinots.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final TokenRecuperacaoRepository tokenRecuperacaoRepository;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, AccessTokenService accessTokenService, TokenRecuperacaoRepository tokenRecuperacaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.tokenRecuperacaoRepository = tokenRecuperacaoRepository;
    }

    public LoginResponseDTO autenticar(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }

        if (!usuario.isAtivo()) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }

        String token = accessTokenService.generateToken(usuario);
        return new LoginResponseDTO(token, usuario.getIdUsuario(), usuario.getNome());
    }



    public void solicitarRecuperacaoSenha(SolicitarRecuperacaoDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElse(null);

        // Não lança exception se o email não existir — mesma lógica anti-enumeração do login
        if (usuario == null) {
            log.info("Solicitação de recuperação para email não cadastrado: {}", dto.email());
            return;
        }

        TokenRecuperacaoSenha tokenRecuperacao = new TokenRecuperacaoSenha();
        tokenRecuperacao.setUsuario(usuario);
        tokenRecuperacao.setToken(UUID.randomUUID().toString());
        tokenRecuperacao.setDataExpiracao(Instant.now().plusSeconds(1800)); // 30 min
        tokenRecuperacaoRepository.save(tokenRecuperacao);

        log.info("Token de recuperação gerado para {}: {}", usuario.getEmail(), tokenRecuperacao.getToken());
    }

    public void redefinirSenhaComToken(RedefinirSenhaComTokenDTO dto) {
        TokenRecuperacaoSenha tokenRecuperacao = tokenRecuperacaoRepository.findByToken(dto.token())
                .orElseThrow(() -> new TokenInvalidoException("Token inválido"));

        if (tokenRecuperacao.isUsado()) {
            throw new TokenInvalidoException("Token já utilizado");
        }

        if (tokenRecuperacao.getDataExpiracao().isBefore(Instant.now())) {
            throw new TokenInvalidoException("Token expirado");
        }

        Usuario usuario = tokenRecuperacao.getUsuario();
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);

        tokenRecuperacao.setUsado(true);
        tokenRecuperacaoRepository.save(tokenRecuperacao);

        log.info("Senha redefinida com sucesso para usuário {}", usuario.getEmail());
    }
}