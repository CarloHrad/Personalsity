package com.example.trinots.VerificacaoServiceTest;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.domain.extra.CodigoVerificacao;
import com.example.trinots.exception.exceptions.TokenInvalidoException;
import com.example.trinots.repository.CodigoVerificacaoRepository;
import com.example.trinots.service.EmailService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificacaoService")
class VerificacaoServiceTest {

    private CodigoVerificacaoRepository codigoRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private VerificacaoService verificacaoService;

    private UUID idUsuario;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        codigoRepository = mock(CodigoVerificacaoRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        verificacaoService = new VerificacaoService(codigoRepository, passwordEncoder, emailService);

        idUsuario = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNome("João");
        usuario.setEmail("joao@email.com");
    }

    private CodigoVerificacao codigoAtivo(Instant expiracao, int tentativas) {
        CodigoVerificacao codigo = new CodigoVerificacao();
        codigo.setUsuario(usuario);
        codigo.setTipo(TipoCodigoEnum.RECUPERACAO_SENHA);
        codigo.setCodigoHash("HASH");
        codigo.setDataExpiracao(expiracao);
        codigo.setTentativas(tentativas);
        return codigo;
    }

    private void stubCodigoAtivo(CodigoVerificacao codigo) {
        when(codigoRepository.findFirstByUsuarioIdUsuarioAndTipoAndUsadoFalseOrderByDataExpiracaoDesc(
                idUsuario, TipoCodigoEnum.RECUPERACAO_SENHA)).thenReturn(Optional.ofNullable(codigo));
    }

    @Nested
    @DisplayName("gerarEEnviar")
    class GerarEEnviar {

        @Test
        @DisplayName("deve gerar código de 6 dígitos, guardar só o hash, validar por 30 minutos e enviar por e-mail")
        void deveGerarGuardarHashEEnviar() {
            when(codigoRepository.findByUsuarioIdUsuarioAndTipoAndUsadoFalse(idUsuario, TipoCodigoEnum.RECUPERACAO_SENHA))
                    .thenReturn(List.of());
            when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
            ArgumentCaptor<CodigoVerificacao> salvo = ArgumentCaptor.forClass(CodigoVerificacao.class);
            when(codigoRepository.save(salvo.capture())).thenAnswer(inv -> inv.getArgument(0));

            Instant antes = Instant.now();
            verificacaoService.gerarEEnviar(usuario, TipoCodigoEnum.RECUPERACAO_SENHA);
            Instant depois = Instant.now();

            ArgumentCaptor<String> codigoEnviado = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviarCodigo(eq(usuario), eq(TipoCodigoEnum.RECUPERACAO_SENHA), codigoEnviado.capture(), eq(30));

            assertThat(codigoEnviado.getValue()).matches("\\d{6}");
            assertThat(salvo.getValue().getCodigoHash()).isEqualTo("hash:" + codigoEnviado.getValue());
            assertThat(salvo.getValue().getCodigoHash()).isNotEqualTo(codigoEnviado.getValue());
            assertThat(salvo.getValue().getTipo()).isEqualTo(TipoCodigoEnum.RECUPERACAO_SENHA);
            assertThat(salvo.getValue().isUsado()).isFalse();
            assertThat(salvo.getValue().getDataExpiracao())
                    .isBetween(antes.plus(30, ChronoUnit.MINUTES), depois.plus(30, ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("deve invalidar os códigos anteriores ainda ativos do mesmo tipo")
        void deveInvalidarCodigosAnteriores() {
            CodigoVerificacao anterior = codigoAtivo(Instant.now().plus(10, ChronoUnit.MINUTES), 0);
            when(codigoRepository.findByUsuarioIdUsuarioAndTipoAndUsadoFalse(idUsuario, TipoCodigoEnum.RECUPERACAO_SENHA))
                    .thenReturn(List.of(anterior));
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(codigoRepository.save(any(CodigoVerificacao.class))).thenAnswer(inv -> inv.getArgument(0));

            verificacaoService.gerarEEnviar(usuario, TipoCodigoEnum.RECUPERACAO_SENHA);

            assertThat(anterior.isUsado()).isTrue();
        }
    }

    @Nested
    @DisplayName("validarEConsumir")
    class ValidarEConsumir {

        @Test
        @DisplayName("código correto e dentro do prazo é aceito e marcado como usado")
        void codigoCorretoEAceito() {
            CodigoVerificacao codigo = codigoAtivo(Instant.now().plus(10, ChronoUnit.MINUTES), 0);
            stubCodigoAtivo(codigo);
            when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);

            verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "123456");

            assertThat(codigo.isUsado()).isTrue();
            verify(codigoRepository).save(codigo);
        }

        @Test
        @DisplayName("sem código ativo, lança TokenInvalidoException")
        void semCodigoAtivo() {
            stubCodigoAtivo(null);

            assertThatThrownBy(() -> verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "123456"))
                    .isInstanceOf(TokenInvalidoException.class);
        }

        @Test
        @DisplayName("código expirado é rejeitado e invalidado")
        void codigoExpirado() {
            CodigoVerificacao codigo = codigoAtivo(Instant.now().minus(1, ChronoUnit.MINUTES), 0);
            stubCodigoAtivo(codigo);

            assertThatThrownBy(() -> verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "123456"))
                    .isInstanceOf(TokenInvalidoException.class);

            assertThat(codigo.isUsado()).isTrue();
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("código errado conta uma tentativa e não consome o código")
        void codigoErradoContaTentativa() {
            CodigoVerificacao codigo = codigoAtivo(Instant.now().plus(10, ChronoUnit.MINUTES), 2);
            stubCodigoAtivo(codigo);
            when(passwordEncoder.matches("000000", "HASH")).thenReturn(false);

            assertThatThrownBy(() -> verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "000000"))
                    .isInstanceOf(TokenInvalidoException.class);

            assertThat(codigo.getTentativas()).isEqualTo(3);
            assertThat(codigo.isUsado()).isFalse();
            verify(codigoRepository).save(codigo);
        }

        @Test
        @DisplayName("após 5 tentativas erradas o código fica bloqueado, mesmo que o valor agora esteja certo")
        void codigoBloqueadoPorTentativas() {
            CodigoVerificacao codigo = codigoAtivo(Instant.now().plus(10, ChronoUnit.MINUTES), VerificacaoService.MAX_TENTATIVAS);
            stubCodigoAtivo(codigo);

            assertThatThrownBy(() -> verificacaoService.validarEConsumir(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "123456"))
                    .isInstanceOf(TokenInvalidoException.class);

            verify(passwordEncoder, never()).matches(any(), any());
            assertThat(codigo.isUsado()).isFalse();
        }
    }
}
