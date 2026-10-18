package com.example.trinots.AuthServiceTest;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.domain.extra.TokenRevogado;
import com.example.trinots.dto.AuthDTO.*;
import com.example.trinots.exception.exceptions.ContaDesativadaException;
import com.example.trinots.exception.exceptions.CredenciaisInvalidasException;
import com.example.trinots.exception.exceptions.EmailNaoVerificadoException;
import com.example.trinots.exception.exceptions.TokenInvalidoException;
import com.example.trinots.repository.TokenRevogadoRepository;
import com.example.trinots.repository.UsuarioRepository;
import com.example.trinots.service.AccessTokenService;
import com.example.trinots.service.AuthService;
import com.example.trinots.service.VerificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private AccessTokenService accessTokenService;
    private VerificacaoService verificacaoService;
    private TokenRevogadoRepository tokenRevogadoRepository;
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        accessTokenService = mock(AccessTokenService.class);
        verificacaoService = mock(VerificacaoService.class);
        tokenRevogadoRepository = mock(TokenRevogadoRepository.class);
        authService = new AuthService(usuarioRepository, passwordEncoder, accessTokenService,
                verificacaoService, tokenRevogadoRepository);

        usuario = new Usuario();
        usuario.setIdUsuario(UUID.randomUUID());
        usuario.setNome("João");
        usuario.setEmail("joao@email.com");
        usuario.setSenha("HASH");
        usuario.setAtivo(true);
        usuario.setEmailVerificado(true);
    }

    private void usuarioExiste() {
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario));
    }

    private void senhaConfere(boolean confere) {
        when(passwordEncoder.matches("senha123", "HASH")).thenReturn(confere);
    }

    @Nested
    @DisplayName("autenticar")
    class Autenticar {

        private final LoginRequestDTO dto = new LoginRequestDTO("joao@email.com", "senha123");

        @Test
        @DisplayName("deve autenticar conta verificada e ativa e devolver o token")
        void deveAutenticar() {
            usuarioExiste();
            senhaConfere(true);
            when(accessTokenService.generateToken(usuario)).thenReturn("abc");

            LoginResponseDTO resposta = authService.autenticar(dto);

            assertThat(resposta.token()).isEqualTo("Bearer abc");
            assertThat(resposta.idUsuario()).isEqualTo(usuario.getIdUsuario());
        }

        @Test
        @DisplayName("e-mail inexistente e senha errada geram a mesma exceção")
        void credenciaisInvalidas() {
            when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.autenticar(dto)).isInstanceOf(CredenciaisInvalidasException.class);

            usuarioExiste();
            senhaConfere(false);
            assertThatThrownBy(() -> authService.autenticar(dto)).isInstanceOf(CredenciaisInvalidasException.class);

            verify(accessTokenService, never()).generateToken(any());
        }

        @Test
        @DisplayName("e-mail não verificado bloqueia o login")
        void emailNaoVerificado() {
            usuario.setEmailVerificado(false);
            usuarioExiste();
            senhaConfere(true);

            assertThatThrownBy(() -> authService.autenticar(dto)).isInstanceOf(EmailNaoVerificadoException.class);
            verify(accessTokenService, never()).generateToken(any());
        }

        @Test
        @DisplayName("com senha errada, não revela se o e-mail da conta está verificado")
        void senhaErradaNaoVazaEstadoDaConta() {
            usuario.setEmailVerificado(false);
            usuario.setAtivo(false);
            usuarioExiste();
            senhaConfere(false);

            assertThatThrownBy(() -> authService.autenticar(dto)).isInstanceOf(CredenciaisInvalidasException.class);
        }

        @Test
        @DisplayName("conta desativada bloqueia o login")
        void contaDesativada() {
            usuario.setAtivo(false);
            usuarioExiste();
            senhaConfere(true);

            assertThatThrownBy(() -> authService.autenticar(dto)).isInstanceOf(ContaDesativadaException.class);
            verify(accessTokenService, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("deve revogar o jti do token até a data em que ele expiraria")
        void deveRevogarToken() {
            DecodedJWT jwt = mock(DecodedJWT.class);
            Instant expiracao = Instant.now().plusSeconds(3600);
            when(jwt.getId()).thenReturn("jti-123");
            when(jwt.getExpiresAt()).thenReturn(Date.from(expiracao));
            when(accessTokenService.decodificar("abc")).thenReturn(jwt);
            ArgumentCaptor<TokenRevogado> captor = ArgumentCaptor.forClass(TokenRevogado.class);

            authService.logout("Bearer abc");

            verify(tokenRevogadoRepository).save(captor.capture());
            assertThat(captor.getValue().getJti()).isEqualTo("jti-123");
            assertThat(captor.getValue().getDataExpiracao()).isEqualTo(expiracao.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("sem header Bearer ou com token inválido, não faz nada e não falha")
        void entradasInvalidas() {
            when(accessTokenService.decodificar("lixo")).thenReturn(null);

            authService.logout(null);
            authService.logout("Basic xyz");
            authService.logout("Bearer lixo");

            verify(tokenRevogadoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("recuperação de senha")
    class RecuperacaoDeSenha {

        @Test
        @DisplayName("deve gerar e enviar código quando o e-mail existe")
        void deveEnviarCodigo() {
            usuarioExiste();

            authService.solicitarRecuperacaoSenha(new SolicitarRecuperacaoDTO("joao@email.com"));

            verify(verificacaoService).gerarEEnviar(usuario, TipoCodigoEnum.RECUPERACAO_SENHA);
        }

        @Test
        @DisplayName("e-mail inexistente: não falha e não envia nada (anti-enumeração)")
        void emailInexistente() {
            when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

            authService.solicitarRecuperacaoSenha(new SolicitarRecuperacaoDTO("joao@email.com"));

            verifyNoInteractions(verificacaoService);
        }

        @Test
        @DisplayName("deve redefinir a senha quando o código é válido")
        void deveRedefinirSenha() {
            usuarioExiste();
            when(passwordEncoder.encode("novaSenha1")).thenReturn("NOVO_HASH");

            authService.redefinirSenhaComToken(new RedefinirSenhaComTokenDTO("joao@email.com", "123456", "novaSenha1"));

            verify(verificacaoService).validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "123456");
            assertThat(usuario.getSenha()).isEqualTo("NOVO_HASH");
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("código inválido não altera a senha")
        void codigoInvalido() {
            usuarioExiste();
            doThrow(new TokenInvalidoException("Código inválido ou expirado"))
                    .when(verificacaoService).validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "000000");

            assertThatThrownBy(() -> authService.redefinirSenhaComToken(
                    new RedefinirSenhaComTokenDTO("joao@email.com", "000000", "novaSenha1")))
                    .isInstanceOf(TokenInvalidoException.class);

            assertThat(usuario.getSenha()).isEqualTo("HASH");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("e-mail inexistente na redefinição responde igual a código inválido")
        void redefinirComEmailInexistente() {
            when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.redefinirSenhaComToken(
                    new RedefinirSenhaComTokenDTO("joao@email.com", "123456", "novaSenha1")))
                    .isInstanceOf(TokenInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("verificação de e-mail")
    class VerificacaoDeEmail {

        @BeforeEach
        void naoVerificado() {
            usuario.setEmailVerificado(false);
        }

        @Test
        @DisplayName("código válido marca o e-mail como verificado")
        void deveVerificar() {
            usuarioExiste();

            authService.verificarEmail(new VerificarEmailDTO("joao@email.com", "123456"));

            verify(verificacaoService).validarEConsumir(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456");
            assertThat(usuario.isEmailVerificado()).isTrue();
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("código inválido mantém o e-mail como não verificado")
        void codigoInvalido() {
            usuarioExiste();
            doThrow(new TokenInvalidoException("Código inválido ou expirado"))
                    .when(verificacaoService).validarEConsumir(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "000000");

            assertThatThrownBy(() -> authService.verificarEmail(new VerificarEmailDTO("joao@email.com", "000000")))
                    .isInstanceOf(TokenInvalidoException.class);

            assertThat(usuario.isEmailVerificado()).isFalse();
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("reenviar: gera novo código quando a conta ainda não foi verificada")
        void reenviar() {
            usuarioExiste();

            authService.reenviarVerificacao(new ReenviarVerificacaoDTO("joao@email.com"));

            verify(verificacaoService).gerarEEnviar(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL);
        }

        @Test
        @DisplayName("reenviar: e-mail já verificado ou inexistente não envia nada nem revela diferença")
        void reenviarSemEfeito() {
            usuario.setEmailVerificado(true);
            usuarioExiste();
            authService.reenviarVerificacao(new ReenviarVerificacaoDTO("joao@email.com"));

            when(usuarioRepository.findByEmail("outro@email.com")).thenReturn(Optional.empty());
            authService.reenviarVerificacao(new ReenviarVerificacaoDTO("outro@email.com"));

            verifyNoInteractions(verificacaoService);
        }
    }

    @Nested
    @DisplayName("reativarConta")
    class ReativarConta {

        private final LoginRequestDTO dto = new LoginRequestDTO("joao@email.com", "senha123");

        @Test
        @DisplayName("credenciais corretas reativam a conta desativada")
        void deveReativar() {
            usuario.setAtivo(false);
            usuarioExiste();
            senhaConfere(true);

            authService.reativarConta(dto);

            assertThat(usuario.isAtivo()).isTrue();
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("senha errada não reativa a conta")
        void senhaErrada() {
            usuario.setAtivo(false);
            usuarioExiste();
            senhaConfere(false);

            assertThatThrownBy(() -> authService.reativarConta(dto)).isInstanceOf(CredenciaisInvalidasException.class);

            assertThat(usuario.isAtivo()).isFalse();
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("conta já ativa: nada a fazer")
        void jaAtiva() {
            usuarioExiste();
            senhaConfere(true);

            authService.reativarConta(dto);

            verify(usuarioRepository, never()).save(any());
        }
    }
}
