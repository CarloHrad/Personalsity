package com.example.trinots.api;

import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.service.EmailService;
import com.jayway.jsonpath.JsonPath;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes "estilo Postman": sobe a aplicacao inteira (Spring + Security + JPA/H2 do perfil desenv)
 * e dispara requisicoes HTTP reais contra os controllers via MockMvc.
 * O unico componente trocado e o EmailService (para capturar o codigo de 6 digitos que iria por e-mail).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("desenv")
abstract class ApiTestBase {

    protected static final String SENHA = "senha123";
    protected static final String UUID_INEXISTENTE = "00000000-0000-0000-0000-000000000000";

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected EmailService emailService;

    protected ResultActions enviar(HttpMethod metodo, String url, String corpo, String authorization) throws Exception {
        MockHttpServletRequestBuilder b = request(metodo, url);
        if (corpo != null) b.contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (authorization != null) b.header(HttpHeaders.AUTHORIZATION, authorization);
        return mvc.perform(b);
    }

    protected ResultActions get(String url, Sessao s) throws Exception {
        return enviar(HttpMethod.GET, url, null, s == null ? null : s.authorization());
    }

    protected ResultActions post(String url, String corpo, Sessao s) throws Exception {
        return enviar(HttpMethod.POST, url, corpo, s == null ? null : s.authorization());
    }

    protected ResultActions put(String url, String corpo, Sessao s) throws Exception {
        return enviar(HttpMethod.PUT, url, corpo, s == null ? null : s.authorization());
    }

    protected ResultActions patch(String url, String corpo, Sessao s) throws Exception {
        return enviar(HttpMethod.PATCH, url, corpo, s == null ? null : s.authorization());
    }

    protected ResultActions delete(String url, String corpo, Sessao s) throws Exception {
        return enviar(HttpMethod.DELETE, url, corpo, s == null ? null : s.authorization());
    }

    protected static String corpo(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    protected static <T> T campo(ResultActions r, String jsonPath) throws Exception {
        return JsonPath.read(corpo(r), jsonPath);
    }

    protected static int statusDe(ResultActions r) {
        return r.andReturn().getResponse().getStatus();
    }

    protected static void assertSemErro5xx(ResultActions r, String contexto) {
        int s = statusDe(r);
        assertTrue(s < 500, contexto + " -> HTTP " + s + " (esperado < 500). Corpo: " + safeBody(r));
    }

    protected static String safeBody(ResultActions r) {
        try { return corpo(r); } catch (Exception e) { return "<ilegivel>"; }
    }

    protected static void assertStatusEm(ResultActions r, String contexto, int... aceitos) {
        int s = statusDe(r);
        for (int a : aceitos) if (a == s) return;
        throw new AssertionError(contexto + " -> HTTP " + s + " fora de " + java.util.Arrays.toString(aceitos)
                + ". Corpo: " + safeBody(r));
    }

    protected record Sessao(String email, String senha, UUID idUsuario, String authorization) {}

    protected static String emailUnico() {
        return "u" + UUID.randomUUID().toString().replace("-", "") + "@exemplo.com";
    }

    protected static Map<String, Object> usuarioPadrao(String email) {
        return Json.mapa(
                "nome", "Ana", "sobrenome", "Silva", "email", email, "senha", SENHA,
                "curso", Json.mapa("nomeCurso", "Ciência da Computação", "instituicao", "UFMG", "duracao", 8),
                "semestreAtual", 3);
    }

    protected ResultActions cadastrarSemVerificar(String email) throws Exception {
        return post("/usuarios", Json.escrever(usuarioPadrao(email)), null);
    }

    protected String ultimoCodigo(String email, TipoCodigoEnum tipo) {
        String achado = null;
        for (Invocation inv : Mockito.mockingDetails(emailService).getInvocations()) {
            if (!inv.getMethod().getName().equals("enviarCodigo")) continue;
            Object[] a = inv.getArguments();
            com.example.trinots.domain.Usuario u = (com.example.trinots.domain.Usuario) a[0];
            if (email.equalsIgnoreCase(u.getEmail()) && a[1] == tipo) achado = (String) a[2];
        }
        assertTrue(achado != null, "Nenhum codigo " + tipo + " foi enviado para " + email);
        return achado;
    }

    protected Sessao novaSessao() throws Exception {
        String email = emailUnico();
        ResultActions criar = cadastrarSemVerificar(email);
        criar.andExpect(status().isCreated());
        String codigo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        post("/auth/verificar-email", Json.obj("email", email, "codigo", codigo), null).andExpect(status().isNoContent());
        return login(email, SENHA);
    }

    protected Sessao login(String email, String senha) throws Exception {
        ResultActions r = post("/auth/login", Json.obj("email", email, "senha", senha), null);
        r.andExpect(status().isOk());
        String token = campo(r, "$.token");
        String id = campo(r, "$.idUsuario");
        return new Sessao(email, senha, UUID.fromString(id), token);
    }

    protected static Map<String, Object> horarioEmbutido(String dia, String ini, String fim) {
        return Json.mapa("diaSemana", dia, "horaInicio", ini, "horaFim", fim);
    }

    protected static Map<String, Object> disciplinaPadrao(String nome) {
        return Json.mapa("nomeDisciplina", nome, "periodo", 3, "professor", "Dr. João da Silva", "sala", "A101",
                "andar", 1, "cor", "#FF5733", "tipoMedia", "SIMPLES", "horarios", List.of());
    }

    protected String criarDisciplina(Sessao s, Map<String, Object> corpo) throws Exception {
        ResultActions r = post("/disciplinas", Json.escrever(corpo), s);
        r.andExpect(status().isCreated());
        return campo(r, "$.idDisciplina");
    }

    protected String criarDisciplina(Sessao s, String nome) throws Exception {
        return criarDisciplina(s, disciplinaPadrao(nome));
    }

    protected static Map<String, Object> tarefaPadrao(String idDisciplina) {
        return Json.mapa("nomeTarefa", "Lista 1", "descricao", "Resolver os exercícios", "dataEntrega", "2030-05-20",
                "horaEntrega", "23:59:00", "idDisciplina", idDisciplina);
    }

    protected String criarTarefa(Sessao s, String idDisciplina) throws Exception {
        ResultActions r = post("/tarefas", Json.escrever(tarefaPadrao(idDisciplina)), s);
        r.andExpect(status().isCreated());
        return campo(r, "$.idTarefa");
    }

    protected static Map<String, Object> avaliacaoPadrao(String idDisciplina) {
        return Json.mapa("nomeAvaliacao", "Prova 1", "descricao", "Capítulos 1 a 3", "dataAvaliacao", "2030-06-10",
                "notaObtida", null, "notaMaxima", 10.0, "peso", 1.0, "idDisciplina", idDisciplina);
    }

    protected String criarAvaliacao(Sessao s, String idDisciplina) throws Exception {
        ResultActions r = post("/avaliacoes", Json.escrever(avaliacaoPadrao(idDisciplina)), s);
        r.andExpect(status().isCreated());
        return campo(r, "$.idAvaliacao");
    }

    protected static MvcResult resultado(ResultActions r) {
        return r.andReturn();
    }
}
