package com.example.trinots.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.trinots.logging.RequisicaoLogFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Health check, X-Request-Id e log de acesso. */
class LogsEHealthApiTest extends ApiTestBase {

    private ListAppender<ILoggingEvent> eventos;
    private Logger loggerAcesso;

    @BeforeEach
    void capturarLog() {
        loggerAcesso = (Logger) LoggerFactory.getLogger(RequisicaoLogFilter.class);
        eventos = new ListAppender<>();
        eventos.start();
        loggerAcesso.addAppender(eventos);
    }

    @AfterEach
    void soltarLog() {
        loggerAcesso.detachAppender(eventos);
    }

    // 'get' do ApiTestBase (url, sessao) esconde o import estatico do MockMvcRequestBuilders; por isso este atalho.
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url);
    }

    // ------------------------------------------------------------------ /health
    @Test
    void healthEPublicoERespondeUp() throws Exception {
        get("/health", null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthNaoExigeTokenMesmoComTokenLixo() throws Exception {
        enviar(org.springframework.http.HttpMethod.GET, "/health", null, "Bearer lixo.lixo.lixo")
                .andExpect(status().isOk());
    }

    @Test
    void healthBemSucedidoNaoPoluiOLog() throws Exception {
        get("/health", null).andExpect(status().isOk());
        assertTrue(eventos.list.stream().noneMatch(e -> e.getFormattedMessage().contains("/health")),
                "GET /health 200 nao deveria gerar linha de log");
    }

    // ------------------------------------------------------------------ X-Request-Id
    @Test
    void todaRespostaTemXRequestId() throws Exception {
        ResultActions r = get("/health", null);
        String id = r.andReturn().getResponse().getHeader(RequisicaoLogFilter.HEADER_REQUEST_ID);
        assertNotNull(id);
        assertTrue(id.matches("^[A-Za-z0-9._-]{8,64}$"), id);
    }

    @Test
    void respostaDe401TambemTemXRequestId() throws Exception {
        ResultActions r = get("/disciplinas", null);
        r.andExpect(status().isUnauthorized());
        assertNotNull(r.andReturn().getResponse().getHeader(RequisicaoLogFilter.HEADER_REQUEST_ID));
    }

    @Test
    void requestIdSeguroEnviadoPeloClienteEhDevolvido() throws Exception {
        String r = mvc.perform(builder("/health").header(RequisicaoLogFilter.HEADER_REQUEST_ID, "app-req-12345"))
                .andReturn().getResponse().getHeader(RequisicaoLogFilter.HEADER_REQUEST_ID);
        assertEquals("app-req-12345", r);
    }

    @Test
    void requestIdPerigosoEhSubstituidoPorUmNovo() throws Exception {
        String r = mvc.perform(builder("/health").header(RequisicaoLogFilter.HEADER_REQUEST_ID, "<script>alert(1)</script>"))
                .andReturn().getResponse().getHeader(RequisicaoLogFilter.HEADER_REQUEST_ID);
        assertNotNull(r);
        assertNotEquals("<script>alert(1)</script>", r);
        assertTrue(r.matches("^[A-Za-z0-9._-]{8,64}$"), r);
    }

    // ------------------------------------------------------------------ log de acesso
    @Test
    void semTokenGera401EmWarnComMetodoECaminho() throws Exception {
        get("/disciplinas", null).andExpect(status().isUnauthorized());
        ILoggingEvent e = eventos.list.stream()
                .filter(x -> x.getFormattedMessage().contains("/disciplinas")).findFirst().orElseThrow();
        assertEquals(Level.WARN, e.getLevel());
        assertTrue(e.getFormattedMessage().startsWith("GET /disciplinas 401 "), e.getFormattedMessage());
    }

    @Test
    void loginComSenhaErradaGera401EmWarnSemVazarCredenciais() throws Exception {
        Sessao s = novaSessao();
        post("/auth/login", Json.obj("email", s.email(), "senha", "senhaErrada999"), null)
                .andExpect(status().isUnauthorized());
        ILoggingEvent e = eventos.list.stream()
                .filter(x -> x.getFormattedMessage().contains("/auth/login")).reduce((a, b) -> b).orElseThrow();
        assertEquals(Level.WARN, e.getLevel());
        String linha = e.getFormattedMessage();
        assertFalse(linha.contains("senhaErrada999"), linha);
        assertFalse(linha.contains(s.email()), linha);
    }

    @Test
    void requisicaoAutenticadaRegistraOIdDoUsuarioSemOToken() throws Exception {
        Sessao s = novaSessao();
        get("/disciplinas", s).andExpect(status().isOk());
        ILoggingEvent e = eventos.list.stream()
                .filter(x -> x.getFormattedMessage().contains("GET /disciplinas 200")).reduce((a, b) -> b).orElseThrow();
        assertEquals(Level.INFO, e.getLevel());
        assertTrue(e.getFormattedMessage().contains("user=" + s.idUsuario()), e.getFormattedMessage());
        assertFalse(e.getFormattedMessage().contains("Bearer"), e.getFormattedMessage());
        assertFalse(e.getFormattedMessage().contains(s.authorization().substring(7, 40)), e.getFormattedMessage());
    }

    @Test
    void queryStringNaoVaiParaOLog() throws Exception {
        mvc.perform(builder("/disciplinas?segredo=valorsensivel123").header(HttpHeaders.AUTHORIZATION, "Bearer x"));
        assertTrue(eventos.list.stream().noneMatch(x -> x.getFormattedMessage().contains("valorsensivel123")));
    }
}
