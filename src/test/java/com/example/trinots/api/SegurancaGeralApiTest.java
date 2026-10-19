package com.example.trinots.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comportamento transversal de TODOS os endpoints: sem token, token lixo, "Bearer Bearer", rota inexistente,
 * metodo errado, content-type errado, JSON quebrado, ids que nao sao UUID.
 */
class SegurancaGeralApiTest extends ApiTestBase {

    private static final String ID = "11111111-1111-1111-1111-111111111111";

    /** Os 41 endpoints que exigem autenticacao (48 no total, menos os 7 publicos). */
    static Stream<Arguments> endpointsProtegidos() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/auth/logout"),
                Arguments.of(HttpMethod.PATCH, "/usuarios/senha"),
                Arguments.of(HttpMethod.GET, "/usuarios/me"),
                Arguments.of(HttpMethod.PUT, "/usuarios/me"),
                Arguments.of(HttpMethod.PUT, "/usuarios/curso"),
                Arguments.of(HttpMethod.GET, "/usuarios/configuracoes/media"),
                Arguments.of(HttpMethod.PUT, "/usuarios/configuracoes/media"),
                Arguments.of(HttpMethod.PATCH, "/usuarios/me/desativar"),
                Arguments.of(HttpMethod.DELETE, "/usuarios/me"),
                Arguments.of(HttpMethod.POST, "/disciplinas"),
                Arguments.of(HttpMethod.GET, "/disciplinas/" + ID),
                Arguments.of(HttpMethod.GET, "/disciplinas"),
                Arguments.of(HttpMethod.GET, "/disciplinas/arquivadas"),
                Arguments.of(HttpMethod.GET, "/disciplinas/ativas"),
                Arguments.of(HttpMethod.PUT, "/disciplinas/" + ID),
                Arguments.of(HttpMethod.GET, "/disciplinas/" + ID + "/media"),
                Arguments.of(HttpMethod.PATCH, "/disciplinas/" + ID + "/arquivar"),
                Arguments.of(HttpMethod.PATCH, "/disciplinas/" + ID + "/desarquivar"),
                Arguments.of(HttpMethod.PATCH, "/disciplinas/" + ID + "/status"),
                Arguments.of(HttpMethod.DELETE, "/disciplinas/" + ID),
                Arguments.of(HttpMethod.POST, "/horarios"),
                Arguments.of(HttpMethod.GET, "/horarios/" + ID),
                Arguments.of(HttpMethod.GET, "/horarios/disciplina/" + ID),
                Arguments.of(HttpMethod.PUT, "/horarios/" + ID),
                Arguments.of(HttpMethod.DELETE, "/horarios/" + ID),
                Arguments.of(HttpMethod.POST, "/tarefas"),
                Arguments.of(HttpMethod.GET, "/tarefas/" + ID),
                Arguments.of(HttpMethod.GET, "/tarefas/disciplina/" + ID),
                Arguments.of(HttpMethod.GET, "/tarefas/disciplina/" + ID + "/status?status=PENDENTE"),
                Arguments.of(HttpMethod.GET, "/tarefas?status=PENDENTE"),
                Arguments.of(HttpMethod.PUT, "/tarefas/" + ID),
                Arguments.of(HttpMethod.PATCH, "/tarefas/" + ID + "/concluir"),
                Arguments.of(HttpMethod.PATCH, "/tarefas/" + ID + "/reabrir"),
                Arguments.of(HttpMethod.DELETE, "/tarefas/" + ID),
                Arguments.of(HttpMethod.POST, "/avaliacoes"),
                Arguments.of(HttpMethod.GET, "/avaliacoes/" + ID),
                Arguments.of(HttpMethod.GET, "/avaliacoes/disciplina/" + ID),
                Arguments.of(HttpMethod.PUT, "/avaliacoes/" + ID),
                Arguments.of(HttpMethod.PATCH, "/avaliacoes/" + ID + "/concluir"),
                Arguments.of(HttpMethod.PATCH, "/avaliacoes/" + ID + "/reabrir"),
                Arguments.of(HttpMethod.DELETE, "/avaliacoes/" + ID));
    }

    @ParameterizedTest(name = "{0} {1} sem token -> 401 JSON")
    @MethodSource("endpointsProtegidos")
    void semTokenRetorna401ComCorpoJson(HttpMethod metodo, String url) throws Exception {
        ResultActions r = enviar(metodo, url, "{}", null);
        r.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("401"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        assertTrue(r.andReturn().getResponse().getContentType().startsWith("application/json"));
    }

    @ParameterizedTest(name = "{0} {1} com token lixo -> 401")
    @MethodSource("endpointsProtegidos")
    void tokenLixoRetorna401(HttpMethod metodo, String url) throws Exception {
        enviar(metodo, url, "{}", "Bearer isto.nao.eh.um.jwt").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("401"));
    }

    @ParameterizedTest(name = "Authorization [{0}] -> 401")
    @ValueSource(strings = {"", " ", "Bearer", "Bearer ", "Bearer  ", "bearer abc", "Basic dXNlcjpwYXNz", "Token abc",
            "Bearer Bearer eyJhbGciOiJIUzI1NiJ9.e30.x", "Bearer null", "Bearer undefined", "Bearer '; DROP TABLE usuario;--",
            "Bearer <script>alert(1)</script>", "eyJhbGciOiJIUzI1NiJ9.e30.x"})
    void cabecalhosAuthorizationMalformados(String header) throws Exception {
        enviar(HttpMethod.GET, "/usuarios/me", null, header).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("401"));
    }

    @Test
    void bearerDuplicadoEhRejeitadoEPrefixoFoiDevolvidoPeloLogin() throws Exception {
        Sessao s = novaSessao();
        assertTrue(s.authorization().startsWith("Bearer "), "login devolve o token ja com o prefixo");
        enviar(HttpMethod.GET, "/usuarios/me", null, "Bearer " + s.authorization()).andExpect(status().isUnauthorized());
        get("/usuarios/me", s).andExpect(status().isOk());
    }

    @Test
    void tokenAdulteradoEhRejeitado() throws Exception {
        Sessao s = novaSessao();
        String t = s.authorization();
        String adulterado = t.substring(0, t.length() - 3) + (t.endsWith("AAA") ? "BBB" : "AAA");
        enviar(HttpMethod.GET, "/usuarios/me", null, adulterado).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSemAssinaturaAlgNoneEhRejeitado() throws Exception {
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"qualquer@x.com\",\"iss\":\"auth\",\"jti\":\"x\"}".getBytes());
        enviar(HttpMethod.GET, "/usuarios/me", null, "Bearer " + header + "." + payload + ".").andExpect(status().isUnauthorized());
    }

    @Test
    void rotaInexistente() throws Exception {
        Sessao s = novaSessao();
        ResultActions r = get("/rota/que/nao/existe", s);
        assertEquals(404, statusDe(r));
        r.andExpect(jsonPath("$.codigo").value("404"));
    }

    @ParameterizedTest(name = "metodo nao suportado {0} {1}")
    @MethodSource("metodosErrados")
    void metodoNaoSuportadoRetorna405(HttpMethod metodo, String url) throws Exception {
        Sessao s = novaSessao();
        enviar(metodo, url, null, s.authorization()).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("405"));
    }

    static Stream<Arguments> metodosErrados() {
        return Stream.of(
                Arguments.of(HttpMethod.DELETE, "/auth/logout"),
                Arguments.of(HttpMethod.GET, "/auth/logout"),
                Arguments.of(HttpMethod.POST, "/usuarios/me"),
                Arguments.of(HttpMethod.DELETE, "/disciplinas"),
                Arguments.of(HttpMethod.PUT, "/disciplinas"),
                Arguments.of(HttpMethod.POST, "/disciplinas/" + ID),
                Arguments.of(HttpMethod.PUT, "/disciplinas/" + ID + "/arquivar"),
                Arguments.of(HttpMethod.GET, "/disciplinas/" + ID + "/arquivar"),
                Arguments.of(HttpMethod.POST, "/horarios/" + ID),
                Arguments.of(HttpMethod.PATCH, "/horarios/" + ID),
                Arguments.of(HttpMethod.PUT, "/tarefas"),
                Arguments.of(HttpMethod.POST, "/tarefas/" + ID + "/concluir"),
                Arguments.of(HttpMethod.PUT, "/avaliacoes"),
                Arguments.of(HttpMethod.GET, "/avaliacoes/" + ID + "/concluir"));
    }

    @Test
    void metodoErradoEmRotaPublica() throws Exception {
        enviar(HttpMethod.GET, "/auth/login", null, null).andExpect(status().isMethodNotAllowed());
        enviar(HttpMethod.PUT, "/auth/verificar-email", "{}", null).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void contentTypeNaoJsonRetorna415() throws Exception {
        Sessao s = novaSessao();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/disciplinas")
                        .header("Authorization", s.authorization())
                        .contentType(org.springframework.http.MediaType.TEXT_PLAIN).content("nome=x"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.codigo").value("415"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_XML).content("<a/>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @ParameterizedTest(name = "corpo ilegivel [{0}]")
    @ValueSource(strings = {"", "{", "}", "{\"email\":", "[]", "\"texto\"", "123", "null", "{'email':'a@b.com'}",
            "{\"email\":\"a@b.com\",}", "<xml/>", "\u0000", "{\"a\":\"\\u00zz\"}"})
    void corpoIlegivelNuncaViraErro500(String corpo) throws Exception {
        for (String url : new String[]{"/auth/login", "/auth/esqueci-senha", "/auth/verificar-email", "/usuarios"}) {
            ResultActions r = post(url, corpo, null);
            assertSemErro5xx(r, "POST " + url + " com corpo [" + corpo + "]");
            assertEquals(400, statusDe(r), "POST " + url + " [" + corpo + "] -> " + safeBody(r));
        }
    }

    @ParameterizedTest(name = "corpo ilegivel autenticado [{0}]")
    @ValueSource(strings = {"", "{", "[]", "null", "\"x\"", "{\"nomeDisciplina\":["})
    void corpoIlegivelEmEndpointsAutenticados(String corpo) throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        for (ResultActions r : new ResultActions[]{
                post("/disciplinas", corpo, s), put("/disciplinas/" + id, corpo, s), post("/horarios", corpo, s),
                post("/tarefas", corpo, s), post("/avaliacoes", corpo, s), patch("/usuarios/senha", corpo, s),
                put("/usuarios/me", corpo, s), put("/usuarios/curso", corpo, s), put("/usuarios/configuracoes/media", corpo, s),
                patch("/avaliacoes/" + UUID_INEXISTENTE + "/concluir", corpo, s), delete("/usuarios/me", corpo, s)}) {
            assertEquals(400, statusDe(r), safeBody(r));
        }
    }

    @ParameterizedTest(name = "id invalido [{0}]")
    @ValueSource(strings = {"abc", "123", "null", "undefined", "1' OR '1'='1", "11111111-1111-1111-1111-11111111111",
            "11111111-1111-1111-1111-1111111111111", "gggggggg-gggg-gggg-gggg-gggggggggggg", "<script>alert(1)</script>", "0"})
    void idQueNaoEhUuidRetorna400(String id) throws Exception {
        Sessao s = novaSessao();
        for (String url : new String[]{"/disciplinas/" + id, "/disciplinas/" + id + "/media", "/horarios/" + id,
                "/horarios/disciplina/" + id, "/tarefas/" + id, "/tarefas/disciplina/" + id, "/avaliacoes/" + id,
                "/avaliacoes/disciplina/" + id}) {
            ResultActions r = get(url, s);
            assertSemErro5xx(r, "GET " + url);
            assertStatusEm(r, "GET " + url, 400, 404);
        }
        for (String url : new String[]{"/disciplinas/" + id + "/arquivar", "/disciplinas/" + id + "/desarquivar",
                "/disciplinas/" + id + "/status", "/tarefas/" + id + "/concluir", "/tarefas/" + id + "/reabrir",
                "/avaliacoes/" + id + "/reabrir"}) {
            ResultActions r = patch(url, null, s);
            assertSemErro5xx(r, "PATCH " + url);
        }
        for (String url : new String[]{"/disciplinas/" + id, "/horarios/" + id, "/tarefas/" + id, "/avaliacoes/" + id}) {
            assertSemErro5xx(delete(url, null, s), "DELETE " + url);
        }
    }

    @Test
    void uuidValidoMasInexistenteRetorna404EmTodosOsRecursos() throws Exception {
        Sessao s = novaSessao();
        for (String url : new String[]{"/disciplinas/" + UUID_INEXISTENTE, "/disciplinas/" + UUID_INEXISTENTE + "/media",
                "/horarios/" + UUID_INEXISTENTE, "/tarefas/" + UUID_INEXISTENTE, "/avaliacoes/" + UUID_INEXISTENTE}) {
            get(url, s).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("404"));
        }
        for (String url : new String[]{"/horarios/disciplina/" + UUID_INEXISTENTE, "/tarefas/disciplina/" + UUID_INEXISTENTE,
                "/avaliacoes/disciplina/" + UUID_INEXISTENTE}) {
            assertStatusEm(get(url, s), "GET " + url, 404);
        }
    }

    @Test
    void erroNaoVazaStackTraceNemDetalhesInternos() throws Exception {
        Sessao s = novaSessao();
        String corpo = corpo(get("/disciplinas/" + UUID_INEXISTENTE, s));
        for (String proibido : new String[]{"Exception", "at com.example", "org.springframework", "org.hibernate", "SQL", "java.lang"}) {
            assertFalse(corpo.contains(proibido), "A resposta de erro vazou '" + proibido + "': " + corpo);
        }
    }

    @Test
    void cabecalhosDeSegurancaPresentesNasRespostas() throws Exception {
        Sessao s = novaSessao();
        var resp = get("/usuarios/me", s).andReturn().getResponse();
        assertEquals("nosniff", resp.getHeader("X-Content-Type-Options"));
        assertTrue(resp.getHeader("Cache-Control") != null && resp.getHeader("Cache-Control").contains("no-store"));
    }
}
