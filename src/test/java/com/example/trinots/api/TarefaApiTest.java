package com.example.trinots.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /tarefas: POST, GET (id, por disciplina, por disciplina+status, por status), PUT, PATCH concluir/reabrir, DELETE. */
class TarefaApiTest extends ApiTestBase {

    private Map<String, Object> tarefaCom(String idDisc, String campo, Object valor) {
        return Json.com(tarefaPadrao(idDisc), campo, valor);
    }

    @Test
    void criarTarefaValida() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");

        post("/tarefas", Json.escrever(tarefaPadrao(disc)), s).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeTarefa").value("Lista 1")).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.nomeDisciplina").value("Cálculo I")).andExpect(jsonPath("$.dataEntrega").value("2030-05-20"))
                .andExpect(jsonPath("$.dataConclusao").doesNotExist());
    }

    @Test
    void criarTarefaMinima() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        Map<String, Object> c = Json.com(Json.com(tarefaPadrao(disc), "descricao", null), "horaEntrega", null);
        post("/tarefas", Json.escrever(c), s).andExpect(status().isCreated());
    }

    @Test
    void camposObrigatorios() throws Exception {
        Sessao s = novaSessao();
        post("/tarefas", "{}", s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nomeTarefa").exists())
                .andExpect(jsonPath("$.erros.dataEntrega").exists()).andExpect(jsonPath("$.erros.idDisciplina").exists());
    }

    @ParameterizedTest(name = "nomeTarefa invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "-Lista", "Lista;", "Lista=1", "<script>alert(1)</script>", "' OR '1'='1",
            "'; DROP TABLE tarefa;--", "Robert'); DROP TABLE Students;--", "Lista\n", "Lista\u0000", "😀", "=cmd|' /C calc'!A0",
            "@SUM(A1)", "+1+1", "${jndi:ldap://x/a}", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void nomeTarefaInvalido(String nome) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/tarefas", Json.escrever(tarefaCom(disc, "nomeTarefa", nome)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nomeTarefa").exists());
        get("/tarefas/disciplina/" + disc, s).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest(name = "nomeTarefa aceito [{0}]")
    @ValueSource(strings = {"Lista 1", "Lista 1: Exercícios (cap. 2)", "Entrega_final+extra", "Trabalho A&B", "Prova/Revisão", "D'Água"})
    void nomeTarefaAceito(String nome) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/tarefas", Json.escrever(tarefaCom(disc, "nomeTarefa", nome)), s)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nomeTarefa").value(nome));
    }

    @ParameterizedTest(name = "descricao aceita e gravada literalmente [{0}]")
    @ValueSource(strings = {"'; DROP TABLE tarefa; --", "' OR '1'='1", "<script>alert(1)</script>", "Linha 1\nLinha 2",
            "Preço: R$ 10,50 (50%)", "Emoji 😀", "Acentuação: ção ã é ü", "=cmd|' /C calc'!A0"})
    void descricaoComConteudoEstranhoEhInerte(String texto) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/tarefas", Json.escrever(tarefaCom(disc, "descricao", texto)), s);
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.descricao").value(texto));
        String id = campo(r, "$.idTarefa");
        get("/tarefas/" + id, s).andExpect(jsonPath("$.descricao").value(texto));
        get("/disciplinas/ativas", s).andExpect(status().isOk());
    }

    @Test
    void descricaoNoLimiteEAcima() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/tarefas", Json.escrever(tarefaCom(disc, "descricao", "a".repeat(500))), s).andExpect(status().isCreated());
        post("/tarefas", Json.escrever(tarefaCom(disc, "descricao", "a".repeat(501))), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.descricao").exists());
    }

    @ParameterizedTest(name = "descricao com controle [{0}]")
    @ValueSource(strings = {"texto\u0000nulo", "texto\u0007bell"})
    void descricaoComCaractereDeControle(String texto) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/tarefas", Json.escrever(tarefaCom(disc, "descricao", texto)), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "dataEntrega ilegivel [{0}]")
    @ValueSource(strings = {"\"2030-02-30\"", "\"30/05/2030\"", "\"2030-13-01\"", "\"amanha\"", "\"\"", "null", "true",
            "\"2030-05-20'; DROP TABLE tarefa;--\"", "{}"})
    void dataEntregaInvalida(String data) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/tarefas", Json.escrever(tarefaCom(disc, "dataEntrega", new Json.Cru(data))), s);
        assertSemErro5xx(r, "dataEntrega=" + data);
        r.andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "dataEntrega aceita [{0}]")
    @ValueSource(strings = {"\"2024-02-29\"", "\"2020-01-01\"", "\"2099-12-31\"", "\"1900-01-01\""})
    void dataEntregaPassadaOuFuturaEhAceita(String data) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/tarefas", Json.escrever(tarefaCom(disc, "dataEntrega", new Json.Cru(data))), s).andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "horaEntrega ilegivel [{0}]")
    @ValueSource(strings = {"\"25:00:00\"", "\"24:00:00\"", "\"abc\"", "\"23:59:60\"", "true", "{}"})
    void horaEntregaInvalida(String hora) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/tarefas", Json.escrever(tarefaCom(disc, "horaEntrega", new Json.Cru(hora))), s);
        assertSemErro5xx(r, "horaEntrega=" + hora);
        r.andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "idDisciplina invalido [{0}]")
    @ValueSource(strings = {"\"abc\"", "123", "null", "\"' OR '1'='1\"", "\"\"", "[]"})
    void idDisciplinaInvalido(String id) throws Exception {
        Sessao s = novaSessao();
        post("/tarefas", Json.escrever(tarefaCom("x", "idDisciplina", new Json.Cru(id))), s).andExpect(status().isBadRequest());
    }

    @Test
    void disciplinaInexistenteOuDeOutroUsuario404() throws Exception {
        Sessao s = novaSessao();
        Sessao outro = novaSessao();
        String discOutro = criarDisciplina(outro, "Física I");
        post("/tarefas", Json.escrever(tarefaPadrao(UUID_INEXISTENTE)), s).andExpect(status().isNotFound());
        post("/tarefas", Json.escrever(tarefaPadrao(discOutro)), s).andExpect(status().isNotFound());
        get("/tarefas/disciplina/" + discOutro, outro).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void tarefaComPrazoVencidoEstaAtrasada() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/tarefas", Json.escrever(Json.com(Json.com(tarefaPadrao(disc), "dataEntrega", "2020-01-01"), "horaEntrega", null)), s);
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ATRASADA"));
    }

    @Test
    void tarefaConcluidaNuncaEstaAtrasada() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = campo(post("/tarefas", Json.escrever(Json.com(tarefaPadrao(disc), "dataEntrega", "2020-01-01")), s), "$.idTarefa");
        patch("/tarefas/" + id + "/concluir", null, s).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA")).andExpect(jsonPath("$.dataConclusao").isNotEmpty());
    }

    @ParameterizedTest(name = "X-Timezone [{0}] nunca quebra")
    @ValueSource(strings = {"America/Sao_Paulo", "Asia/Tokyo", "UTC", "Invalido/Fuso", "", "   ", "'; DROP TABLE tarefa;--", "../../etc"})
    void cabecalhoDeFusoHorario(String fuso) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        criarTarefa(s, disc);
        try {
            ResultActions r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tarefas?status=PENDENTE")
                    .header("Authorization", s.authorization()).header("X-Timezone", fuso));
            assertSemErro5xx(r, "X-Timezone=" + fuso);
            r.andExpect(status().isOk());
        } catch (IllegalArgumentException cabecalhoRecusadoPeloProprioMockMvc) {
        }
    }

    @Test
    void buscarPorIdEListar() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        get("/tarefas/" + id, s).andExpect(status().isOk()).andExpect(jsonPath("$.idTarefa").value(id));
        get("/tarefas/disciplina/" + disc, s).andExpect(jsonPath("$.length()").value(1));
        get("/tarefas/disciplina/" + disc + "/status?status=PENDENTE", s).andExpect(jsonPath("$.length()").value(1));
        get("/tarefas/disciplina/" + disc + "/status?status=CONCLUIDA", s).andExpect(jsonPath("$.length()").value(0));
        get("/tarefas?status=PENDENTE", s).andExpect(jsonPath("$.length()").value(1));
        get("/tarefas?status=PENDENTE&periodo=3", s).andExpect(jsonPath("$.length()").value(1));
        get("/tarefas?status=PENDENTE&periodo=4", s).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void statusEhObrigatorioNasListagensPorStatus() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        get("/tarefas", s).andExpect(status().isBadRequest());
        get("/tarefas/disciplina/" + disc + "/status", s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "status invalido [{0}]")
    @ValueSource(strings = {"ABERTA", "pendente", "1", "", "'; DROP TABLE tarefa;--", "<script>"})
    void statusInvalidoNasListagens(String st) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String q = java.net.URLEncoder.encode(st, java.nio.charset.StandardCharsets.UTF_8);
        for (String url : new String[]{"/tarefas?status=" + q, "/tarefas/disciplina/" + disc + "/status?status=" + q}) {
            ResultActions r = get(url, s);
            assertSemErro5xx(r, url);
            r.andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest(name = "periodo invalido [{0}]")
    @ValueSource(strings = {"abc", "1.5", "'; DROP TABLE tarefa;--"})
    void periodoInvalidoNaListagem(String p) throws Exception {
        Sessao s = novaSessao();
        get("/tarefas?status=PENDENTE&periodo=" + java.net.URLEncoder.encode(p, java.nio.charset.StandardCharsets.UTF_8), s)
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarTarefa() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        put("/tarefas/" + id, Json.escrever(Json.com(Json.com(tarefaPadrao(disc), "nomeTarefa", "Lista 2"), "dataEntrega", "2031-01-01")), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeTarefa").value("Lista 2")).andExpect(jsonPath("$.dataEntrega").value("2031-01-01"));
    }

    @Test
    void atualizarTarefaInvalidaNaoAlteraNada() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        put("/tarefas/" + id, Json.escrever(tarefaCom(disc, "nomeTarefa", "<script>")), s).andExpect(status().isBadRequest());
        get("/tarefas/" + id, s).andExpect(jsonPath("$.nomeTarefa").value("Lista 1"));
    }

    @Test
    void tarefaConcluidaNaoPodeSerEditada() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        patch("/tarefas/" + id + "/concluir", null, s).andExpect(status().isOk());
        put("/tarefas/" + id, Json.escrever(tarefaPadrao(disc)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("460"));
    }

    @Test
    void moverTarefaParaDisciplinaDeOutroUsuarioOuInexistente() throws Exception {
        Sessao s = novaSessao();
        Sessao outro = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String discOutro = criarDisciplina(outro, "Física I");
        String id = criarTarefa(s, disc);
        put("/tarefas/" + id, Json.escrever(tarefaPadrao(discOutro)), s).andExpect(status().isNotFound());
        put("/tarefas/" + id, Json.escrever(tarefaPadrao(UUID_INEXISTENTE)), s).andExpect(status().isNotFound());
    }

    @Test
    void concluirEReabrir() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        patch("/tarefas/" + id + "/concluir", null, s).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDA"));
        patch("/tarefas/" + id + "/concluir", null, s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("460"));
        patch("/tarefas/" + id + "/reabrir", null, s).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataConclusao").doesNotExist());
        patch("/tarefas/" + id + "/reabrir", null, s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("461"));
    }

    @Test
    void concluirIgnoraCorpoEnviado() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        patch("/tarefas/" + id + "/concluir", Json.obj("status", "ATRASADA", "concluida", false), s).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void excluirTarefa() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarTarefa(s, disc);
        delete("/tarefas/" + id, null, s).andExpect(status().isNoContent());
        get("/tarefas/" + id, s).andExpect(status().isNotFound());
        delete("/tarefas/" + id, null, s).andExpect(status().isNotFound());
        delete("/disciplinas/" + disc, null, s).andExpect(status().isNoContent());
    }

    @Test
    void isolamentoEntreUsuarios() throws Exception {
        Sessao dono = novaSessao();
        Sessao intruso = novaSessao();
        String disc = criarDisciplina(dono, "Cálculo I");
        String id = criarTarefa(dono, disc);
        get("/tarefas/" + id, intruso).andExpect(status().isNotFound());
        get("/tarefas/disciplina/" + disc, intruso).andExpect(status().isNotFound());
        get("/tarefas/disciplina/" + disc + "/status?status=PENDENTE", intruso).andExpect(status().isNotFound());
        put("/tarefas/" + id, Json.escrever(tarefaPadrao(disc)), intruso).andExpect(status().isNotFound());
        patch("/tarefas/" + id + "/concluir", null, intruso).andExpect(status().isNotFound());
        patch("/tarefas/" + id + "/reabrir", null, intruso).andExpect(status().isNotFound());
        delete("/tarefas/" + id, null, intruso).andExpect(status().isNotFound());
        get("/tarefas?status=PENDENTE", intruso).andExpect(jsonPath("$.length()").value(0));
        get("/tarefas/" + id, dono).andExpect(jsonPath("$.status").value("PENDENTE"));
    }
}
