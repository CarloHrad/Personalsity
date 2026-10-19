package com.example.trinots.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvaliacaoApiTest extends ApiTestBase {

    private Map<String, Object> avCom(String idDisc, String campo, Object valor) {
        return Json.com(avaliacaoPadrao(idDisc), campo, valor);
    }

    private String disciplinaPonderada(Sessao s, String nome) throws Exception {
        return criarDisciplina(s, Json.com(disciplinaPadrao(nome), "tipoMedia", "PONDERADA"));
    }

    @Test
    void criarAvaliacaoPendente() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avaliacaoPadrao(disc)), s).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeAvaliacao").value("Prova 1")).andExpect(jsonPath("$.concluida").value(false))
                .andExpect(jsonPath("$.notaObtida").doesNotExist()).andExpect(jsonPath("$.notaMaxima").value(10.0))
                .andExpect(jsonPath("$.nomeDisciplina").value("Cálculo I"));
    }

    @Test
    void criarAvaliacaoJaComNotaNascendoConcluida() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "notaObtida", 7.5)), s).andExpect(status().isCreated())
                .andExpect(jsonPath("$.concluida").value(true)).andExpect(jsonPath("$.notaObtida").value(7.5))
                .andExpect(jsonPath("$.dataConclusao").isNotEmpty());
    }

    @Test
    void camposObrigatorios() throws Exception {
        Sessao s = novaSessao();
        post("/avaliacoes", "{}", s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nomeAvaliacao").exists()).andExpect(jsonPath("$.erros.dataAvaliacao").exists())
                .andExpect(jsonPath("$.erros.notaMaxima").exists()).andExpect(jsonPath("$.erros.idDisciplina").exists());
    }

    @ParameterizedTest(name = "nomeAvaliacao invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "A"})
    void nomeAvaliacaoInvalido(String nome) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "nomeAvaliacao", nome)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nomeAvaliacao").exists());
    }

    @Test
    void nomeAvaliacaoTamanhoLimite() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "nomeAvaliacao", "a".repeat(150))), s).andExpect(status().isCreated());
        post("/avaliacoes", Json.escrever(avCom(disc, "nomeAvaliacao", "a".repeat(151))), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "nomeAvaliacao hostil e gravado literalmente [{0}]")
    @ValueSource(strings = {"' OR '1'='1", "'; DROP TABLE avaliacao;--", "<script>alert(1)</script>", "=cmd|' /C calc'!A0",
            "Robert'); DROP TABLE Students;--", "😀 Prova", "Prova\nlinha 2"})
    void nomeAvaliacaoNaoTemPatternMasSqlEhInerte(String nome) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = campo(post("/avaliacoes", Json.escrever(avCom(disc, "nomeAvaliacao", nome)), s)
                .andExpect(status().isCreated()), "$.idAvaliacao");
        get("/avaliacoes/" + id, s).andExpect(jsonPath("$.nomeAvaliacao").value(nome));
        get("/avaliacoes/disciplina/" + disc, s).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void descricaoLimite() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "descricao", "a".repeat(500))), s).andExpect(status().isCreated());
        post("/avaliacoes", Json.escrever(avCom(disc, "descricao", "a".repeat(501))), s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao").exists());
    }

    @ParameterizedTest(name = "notaMaxima invalida [{0}]")
    @ValueSource(strings = {"0", "-1", "-0.01", "1000.01", "1001", "10.123", "null", "\"dez\"", "\"\"", "[]", "{}"})
    void notaMaximaInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "notaMaxima", new Json.Cru(v))), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "notaMaxima valida [{0}]")
    @ValueSource(strings = {"0.01", "1", "5", "10", "100", "999.99", "1000"})
    void notaMaximaValida(String v) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "notaMaxima", new Json.Cru(v))), s).andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "notaObtida invalida [{0}]")
    @ValueSource(strings = {"-0.01", "-1", "10.01", "11", "7.555", "99999", "\"sete\"", "{}"})
    void notaObtidaInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "notaObtida", new Json.Cru(v))), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "pesoInvalido [{0}]")
    @ValueSource(strings = {"0", "-1", "100.01", "1000", "1.234", "\"tres\"", "{}"})
    void pesoInvalido(String v) throws Exception {
        Sessao s = novaSessao();
        String disc = disciplinaPonderada(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "peso", new Json.Cru(v))), s).andExpect(status().isBadRequest());
    }

    @Test
    void pesoObrigatorioEmDisciplinaPonderada() throws Exception {
        Sessao s = novaSessao();
        String disc = disciplinaPonderada(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "peso", null)), s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("472"));
    }

    @Test
    void pesoEhIgnoradoEmDisciplinaSimples() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avCom(disc, "peso", 5.0)), s).andExpect(status().isCreated()).andExpect(jsonPath("$.peso").value(1.0));
        post("/avaliacoes", Json.escrever(avCom(disc, "peso", null)), s).andExpect(status().isCreated()).andExpect(jsonPath("$.peso").value(1.0));
    }

    @ParameterizedTest(name = "dataAvaliacao invalida [{0}]")
    @ValueSource(strings = {"\"2030-02-30\"", "\"10/06/2030\"", "\"amanha\"", "\"\"", "null", "true", "{}", "\"2030-06-10'; DROP TABLE avaliacao;--\""})
    void dataAvaliacaoInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/avaliacoes", Json.escrever(avCom(disc, "dataAvaliacao", new Json.Cru(v))), s);
        assertSemErro5xx(r, "dataAvaliacao=" + v);
        r.andExpect(status().isBadRequest());
    }

    @Test
    void notaObtidaMaiorQueMaximaNaCriacao() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(Json.com(avCom(disc, "notaMaxima", 5.0), "notaObtida", 5.01)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.notaObtidaValida").exists());
        post("/avaliacoes", Json.escrever(Json.com(avCom(disc, "notaMaxima", 5.0), "notaObtida", 5.0)), s).andExpect(status().isCreated());
    }

    @Test
    void disciplinaInexistenteOuDeOutroUsuario() throws Exception {
        Sessao s = novaSessao();
        Sessao outro = novaSessao();
        String discOutro = criarDisciplina(outro, "Física I");
        post("/avaliacoes", Json.escrever(avaliacaoPadrao(UUID_INEXISTENTE)), s).andExpect(status().isNotFound());
        post("/avaliacoes", Json.escrever(avaliacaoPadrao(discOutro)), s).andExpect(status().isNotFound());
        get("/avaliacoes/disciplina/" + discOutro, outro).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscarPorIdEListarPorDisciplina() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        get("/avaliacoes/" + id, s).andExpect(status().isOk()).andExpect(jsonPath("$.idAvaliacao").value(id));
        get("/avaliacoes/disciplina/" + disc, s).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void concluirAvaliacao() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 8.5), s).andExpect(status().isOk())
                .andExpect(jsonPath("$.concluida").value(true)).andExpect(jsonPath("$.notaObtida").value(8.5))
                .andExpect(jsonPath("$.dataConclusao").isNotEmpty());
    }

    @Test
    void concluirDuasVezesRetorna400Codigo474() throws Exception {
        Sessao s = novaSessao();
        String id = criarAvaliacao(s, criarDisciplina(s, "Cálculo I"));
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 8.5), s).andExpect(status().isOk());
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 9.0), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("474"));
    }

    @Test
    void notaNoLimiteEAcimaDoMaximoNaConclusao() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 10.01), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("473"));
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 99999.0), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("473"));
        get("/avaliacoes/" + id, s).andExpect(jsonPath("$.concluida").value(false));
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 10.0), s).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "concluir com corpo invalido [{0}]")
    @ValueSource(strings = {"{}", "{\"notaObtida\":null}", "{\"notaObtida\":-1}", "{\"notaObtida\":\"dez\"}", "{\"notaObtida\":[]}", "[]", "{\"notaObtida\":{}}"})
    void concluirComCorpoInvalido(String corpo) throws Exception {
        Sessao s = novaSessao();
        String id = criarAvaliacao(s, criarDisciplina(s, "Cálculo I"));
        patch("/avaliacoes/" + id + "/concluir", corpo, s).andExpect(status().isBadRequest());
        get("/avaliacoes/" + id, s).andExpect(jsonPath("$.concluida").value(false));
    }

    @Test
    void concluirSemCorpo() throws Exception {
        Sessao s = novaSessao();
        String id = criarAvaliacao(s, criarDisciplina(s, "Cálculo I"));
        patch("/avaliacoes/" + id + "/concluir", null, s).andExpect(status().isBadRequest());
    }

    @Test
    void reabrir() throws Exception {
        Sessao s = novaSessao();
        String id = criarAvaliacao(s, criarDisciplina(s, "Cálculo I"));
        patch("/avaliacoes/" + id + "/reabrir", null, s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("475"));
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 8.0), s).andExpect(status().isOk());
        patch("/avaliacoes/" + id + "/reabrir", null, s).andExpect(status().isOk()).andExpect(jsonPath("$.concluida").value(false))
                .andExpect(jsonPath("$.notaObtida").doesNotExist());
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 9.0), s).andExpect(status().isOk()).andExpect(jsonPath("$.notaObtida").value(9.0));
    }

    @Test
    void reabrirRecalculaStatusDaDisciplina() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 9.0), s).andExpect(status().isOk());
        get("/disciplinas/" + disc, s).andExpect(jsonPath("$.status").value("APROVADO"));
        patch("/avaliacoes/" + id + "/reabrir", null, s).andExpect(status().isOk());
        get("/disciplinas/" + disc, s).andExpect(jsonPath("$.status").value("EM_PROGRESSO"));
    }

    @Test
    void novaAvaliacaoPendenteReabreAMediaDaDisciplinaJaFechada() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        patch("/avaliacoes/" + criarAvaliacao(s, disc) + "/concluir", Json.obj("notaObtida", 2.0), s).andExpect(status().isOk());
        get("/disciplinas/" + disc, s).andExpect(jsonPath("$.status").value("REPROVADO"));
        criarAvaliacao(s, disc);
        get("/disciplinas/" + disc, s).andExpect(jsonPath("$.status").value("EM_PROGRESSO"));
    }

    @Test
    void riscoDeReprovacaoQuandoANotaNecessariaPassaDoMaximo() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        patch("/avaliacoes/" + criarAvaliacao(s, disc) + "/concluir", Json.obj("notaObtida", 0.0), s).andExpect(status().isOk());
        criarAvaliacao(s, disc);
        ResultActions r = get("/disciplinas/" + disc + "/media", s);
        r.andExpect(status().isOk()).andExpect(jsonPath("$.emRiscoReprovacao").value(true));
    }

    @Test
    void atualizarAvaliacaoPendente() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        put("/avaliacoes/" + id, Json.escrever(Json.com(Json.com(avaliacaoPadrao(disc), "nomeAvaliacao", "Prova Final"), "notaMaxima", 20.0)), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeAvaliacao").value("Prova Final")).andExpect(jsonPath("$.notaMaxima").value(20.0));
    }

    @Test
    void atualizarPendenteNaoPermiteDefinirNotaPorAqui() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        put("/avaliacoes/" + id, Json.escrever(avCom(disc, "notaObtida", 7.0)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("473"));
    }

    @Test
    void avaliacaoConcluidaSoAceitaMudarNomeEDescricao() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 7.0), s).andExpect(status().isOk());
        put("/avaliacoes/" + id, Json.escrever(Json.com(avaliacaoPadrao(disc), "nomeAvaliacao", "Renomeada")), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeAvaliacao").value("Renomeada")).andExpect(jsonPath("$.notaObtida").value(7.0));
        put("/avaliacoes/" + id, Json.escrever(Json.com(avaliacaoPadrao(disc), "notaMaxima", 20.0)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("474"));
        put("/avaliacoes/" + id, Json.escrever(avCom(disc, "notaObtida", 9.0)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("474"));
    }

    @Test
    void naoMoveAvaliacaoParaOutraDisciplina() throws Exception {
        Sessao s = novaSessao();
        String a = criarDisciplina(s, "Cálculo I");
        String b = criarDisciplina(s, "Física I");
        String id = criarAvaliacao(s, a);
        put("/avaliacoes/" + id, Json.escrever(avaliacaoPadrao(b)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("443"));
    }

    @Test
    void atualizarComPayloadInvalidoNaoAlteraNada() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarAvaliacao(s, disc);
        put("/avaliacoes/" + id, Json.escrever(avCom(disc, "notaMaxima", -5)), s).andExpect(status().isBadRequest());
        put("/avaliacoes/" + id, Json.escrever(avCom(disc, "nomeAvaliacao", "")), s).andExpect(status().isBadRequest());
        get("/avaliacoes/" + id, s).andExpect(jsonPath("$.nomeAvaliacao").value("Prova 1")).andExpect(jsonPath("$.notaMaxima").value(10.0));
    }

    @Test
    void excluirAvaliacaoAtualizaMedia() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String a1 = criarAvaliacao(s, disc);
        String a2 = criarAvaliacao(s, disc);
        patch("/avaliacoes/" + a1 + "/concluir", Json.obj("notaObtida", 9.0), s).andExpect(status().isOk());
        patch("/avaliacoes/" + a2 + "/concluir", Json.obj("notaObtida", 3.0), s).andExpect(status().isOk());
        assertEquals(6.0, ((Number) campo(get("/disciplinas/" + disc + "/media", s), "$.mediaAtual")).doubleValue(), 0.0001);
        delete("/avaliacoes/" + a2, null, s).andExpect(status().isNoContent());
        assertEquals(9.0, ((Number) campo(get("/disciplinas/" + disc + "/media", s), "$.mediaAtual")).doubleValue(), 0.0001);
        delete("/avaliacoes/" + a2, null, s).andExpect(status().isNotFound());
    }

    @Test
    void mediaExatamenteIgualAAprovacaoAprova() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        patch("/avaliacoes/" + criarAvaliacao(s, disc) + "/concluir", Json.obj("notaObtida", 6.0), s).andExpect(status().isOk());
        get("/disciplinas/" + disc, s).andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void isolamentoEntreUsuarios() throws Exception {
        Sessao dono = novaSessao();
        Sessao intruso = novaSessao();
        String disc = criarDisciplina(dono, "Cálculo I");
        String id = criarAvaliacao(dono, disc);
        get("/avaliacoes/" + id, intruso).andExpect(status().isNotFound());
        get("/avaliacoes/disciplina/" + disc, intruso).andExpect(status().isNotFound());
        put("/avaliacoes/" + id, Json.escrever(avaliacaoPadrao(disc)), intruso).andExpect(status().isNotFound());
        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 10.0), intruso).andExpect(status().isNotFound());
        patch("/avaliacoes/" + id + "/reabrir", null, intruso).andExpect(status().isNotFound());
        delete("/avaliacoes/" + id, null, intruso).andExpect(status().isNotFound());
        get("/avaliacoes/" + id, dono).andExpect(jsonPath("$.concluida").value(false));
    }
}
