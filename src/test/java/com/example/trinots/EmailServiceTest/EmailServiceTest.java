package com.example.trinots.EmailServiceTest;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.service.EmailService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmailService")
class EmailServiceTest {

    private HttpServer servidor;
    private final AtomicReference<String> corpo = new AtomicReference<>();
    private final AtomicReference<String> autorizacao = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicInteger chamadas = new AtomicInteger();
    private volatile int statusDaApi = 200;

    private ObjectProvider<JavaMailSender> smtp;
    private Usuario usuario;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/emails", troca -> {
            chamadas.incrementAndGet();
            corpo.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
            contentType.set(troca.getRequestHeaders().getFirst("Content-Type"));
            byte[] resposta = "{}".getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(statusDaApi, resposta.length);
            troca.getResponseBody().write(resposta);
            troca.close();
        });
        servidor.start();

        smtp = mock(ObjectProvider.class);
        usuario = new Usuario();
        usuario.setIdUsuario(UUID.randomUUID());
        usuario.setNome("Ana");
        usuario.setEmail("ana@email.com");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    private String urlDaApi() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/emails";
    }

    private EmailService comApi(String chave) {
        return new EmailService(smtp, "no-reply@trinots.app", false, chave, urlDaApi());
    }

    @Test
    @DisplayName("com chave de API, envia por HTTPS com Bearer, destinatário e código no corpo")
    void enviaPelaApi() {
        comApi("re_chave_de_teste").enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30);

        assertThat(chamadas.get()).isEqualTo(1);
        assertThat(autorizacao.get()).isEqualTo("Bearer re_chave_de_teste");
        assertThat(contentType.get()).isEqualTo("application/json");
        assertThat(corpo.get())
                .contains("\"from\":\"no-reply@trinots.app\"")
                .contains("\"to\":[\"ana@email.com\"]")
                .contains("\"subject\":\"Trinots - Confirme seu e-mail\"")
                .contains("123456");
    }

    @Test
    @DisplayName("e-mail de recuperação de senha usa o assunto próprio")
    void assuntoDaRecuperacao() {
        comApi("re_chave").enviarCodigo(usuario, TipoCodigoEnum.RECUPERACAO_SENHA, "654321", 30);

        assertThat(corpo.get()).contains("redefinir sua senha").contains("654321");
    }

    @Test
    @DisplayName("com chave de API, não usa o SMTP")
    void naoUsaSmtpComApi() {
        comApi("re_chave").enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30);

        verify(smtp, never()).getIfAvailable();
    }

    @Test
    @DisplayName("escapa aspas, barras e quebras de linha no JSON")
    void escapaCaracteresEspeciais() {
        usuario.setNome("Ana \"A\" \\ Silva");

        comApi("re_chave").enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30);

        assertThat(corpo.get())
                .contains("Ana \\\"A\\\" \\\\ Silva")
                .contains("\\n\\n");
        assertThat(corpo.get()).doesNotContain("\n");
    }

    @Test
    @DisplayName("falha da API (HTTP 500) não lança exceção")
    void naoLancaQuandoApiFalha() {
        statusDaApi = 500;

        assertThatCode(() -> comApi("re_chave").enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30))
                .doesNotThrowAnyException();
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("API fora do ar não lança exceção")
    void naoLancaQuandoApiForaDoAr() {
        servidor.stop(0);

        assertThatCode(() -> comApi("re_chave").enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sem chave de API, usa o SMTP quando configurado")
    void usaSmtpSemChave() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(smtp.getIfAvailable()).thenReturn(sender);

        comApi("").enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30);

        verify(sender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
        assertThat(chamadas.get()).isZero();
    }

    @Test
    @DisplayName("sem API e sem SMTP, apenas registra e não lança exceção")
    void semNenhumEnvioConfigurado() {
        when(smtp.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> new EmailService(smtp, "no-reply@trinots.app", true)
                .enviarCodigo(usuario, TipoCodigoEnum.VERIFICACAO_EMAIL, "123456", 30))
                .doesNotThrowAnyException();
        assertThat(chamadas.get()).isZero();
    }
}
