package com.example.trinots.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /disciplinas: POST, GET (id, lista, ativas, arquivadas, media), PUT, PATCH (arquivar, desarquivar, status), DELETE. */
class DisciplinaApiTest extends ApiTestBase {

    private Map<String, Object> corpoCom(String campo, Object valor) {
        return Json.com(disciplinaPadrao("Cálculo " + System.nanoTime()), campo, valor);
    }

    @Test
    void criarDisciplinaComHorariosEmbutidos() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(disciplinaPadrao("Cálculo I"), "horarios", List.of(
                horarioEmbutido("SEGUNDA", "08:00:00", "10:00:00"), horarioEmbutido("QUARTA", "08:00:00", "10:00:00")));
        post("/disciplinas", Json.escrever(c), s).andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeDisciplina").value("Cálculo I")).andExpect(jsonPath("$.horarios.length()").value(2))
                .andExpect(jsonPath("$.status").value("EM_PROGRESSO")).andExpect(jsonPath("$.arquivada").value(false))
                .andExpect(jsonPath("$.tipoMedia").value("SIMPLES"));
    }

    @Test
    void criarDisciplinaMinimaSemHorariosNemAndar() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(Json.com(disciplinaPadrao("Física I"), "horarios", Json.REMOVER), "andar", null);
        post("/disciplinas", Json.escrever(c), s).andExpect(status().isCreated());
    }

    @Test
    void nomeDuplicadoParaOMesmoUsuarioRetorna400Codigo440() throws Exception {
        Sessao s = novaSessao();
        criarDisciplina(s, "Cálculo I");
        post("/disciplinas", Json.escrever(disciplinaPadrao("Cálculo I")), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("440"));
    }

    @Test
    void mesmoNomeEmUsuariosDiferentesEPermitido() throws Exception {
        criarDisciplina(novaSessao(), "Cálculo I");
        criarDisciplina(novaSessao(), "Cálculo I");
    }

    @Test
    void horariosEmbutidosConflitantesEntreSiRetornam400Codigo450() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(disciplinaPadrao("Cálculo I"), "horarios", List.of(
                horarioEmbutido("SEGUNDA", "08:00:00", "10:00:00"), horarioEmbutido("SEGUNDA", "09:00:00", "11:00:00")));
        ResultActions r = post("/disciplinas", Json.escrever(c), s);
        r.andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("450"));
        post("/disciplinas", Json.escrever(disciplinaPadrao("Cálculo I")), s).andExpect(status().isCreated());
    }

    @Test
    void camposObrigatoriosAusentes() throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", "{}", s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nomeDisciplina").exists()).andExpect(jsonPath("$.erros.periodo").exists())
                .andExpect(jsonPath("$.erros.professor").exists()).andExpect(jsonPath("$.erros.sala").exists())
                .andExpect(jsonPath("$.erros.cor").exists()).andExpect(jsonPath("$.erros.tipoMedia").exists());
    }

    @ParameterizedTest(name = "nomeDisciplina invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "-Cálculo", "C++", "Cálculo;", "<script>alert(1)</script>", "' OR '1'='1",
            "'; DROP TABLE disciplina;--", "Robert'); DROP TABLE Students;--", "Cálculo\n", "Cálculo\u0000", "😀",
            "=cmd|' /C calc'!A0", "${jndi:ldap://x/a}"})
    void nomeDisciplinaInvalido(String nome) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(Json.com(disciplinaPadrao("x"), "nomeDisciplina", nome)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nomeDisciplina").exists());
        get("/disciplinas/ativas", s).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest(name = "nomeDisciplina aceito [{0}]")
    @ValueSource(strings = {"Cálculo I", "Algoritmos & Estruturas", "Engenharia de Software (EaD)", "Inglês - Nível 3",
            "Lab/Prática", "D'Água", "Cal' OR 'x' OR '"})
    void nomeDisciplinaAceitoEhGravadoLiteralmente(String nome) throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, nome);
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.nomeDisciplina").value(nome));
    }

    @ParameterizedTest(name = "professor invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "João123", "3João", "João;", "<script>alert(1)</script>", "' OR '1'='1", "João\nSilva",
            "'; DROP TABLE disciplina;--", "😀", "João@x.com", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void professorInvalido(String v) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(corpoCom("professor", v)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.professor").exists());
    }

    @ParameterizedTest(name = "professor aceito [{0}]")
    @ValueSource(strings = {"João", "Dr. João da Silva", "Prof.ª Maria", "O’Brien", "D'Ávila", "José-Luís", "Silva, João",
            "Nguyễn Văn", "Łukasz Żółć", "李雷", "Maria (Coord.)"})
    void professorComCaracteresEspeciaisLegitimos(String v) throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, Json.com(disciplinaPadrao("Cálculo I"), "professor", v));
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.professor").value(v));
    }

    @ParameterizedTest(name = "sala invalida [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "-A1", "A1;", "A1'", "<A1>", "A#1", "AAAAAAAAAAAAAAAAAAAAA", "'; DROP TABLE disciplina;--"})
    void salaInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(corpoCom("sala", v)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.sala").exists());
    }

    @ParameterizedTest(name = "cor invalida [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "FF5733", "#GGGGGG", "#FFFF", "red", "rgb(1,2,3)", "#FF5733;", "'; DROP TABLE disciplina;--", "<script>"})
    void corInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(corpoCom("cor", v)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.cor").exists());
    }

    @ParameterizedTest(name = "periodo invalido [{0}]")
    @ValueSource(strings = {"0", "-1", "21", "null", "\"um\"", "99999999999"})
    void periodoInvalido(String v) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(corpoCom("periodo", new Json.Cru(v))), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "andar invalido [{0}]")
    @ValueSource(strings = {"-1", "201", "\"terreo\""})
    void andarInvalido(String v) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(corpoCom("andar", new Json.Cru(v))), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "tipoMedia invalido [{0}]")
    @ValueSource(strings = {"\"GEOMETRICA\"", "\"simples\"", "\"\"", "null", "\"SIMPLES; DROP TABLE x\""})
    void tipoMediaInvalido(String v) throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(corpoCom("tipoMedia", new Json.Cru(v))), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "horario embutido invalido #{index}")
    @ValueSource(strings = {
            "{\"diaSemana\":\"FERIADO\",\"horaInicio\":\"08:00:00\",\"horaFim\":\"10:00:00\"}",
            "{\"diaSemana\":\"SEGUNDA\",\"horaInicio\":\"25:00:00\",\"horaFim\":\"10:00:00\"}",
            "{\"diaSemana\":\"SEGUNDA\",\"horaInicio\":\"oito\",\"horaFim\":\"10:00:00\"}",
            "{\"diaSemana\":\"SEGUNDA\",\"horaInicio\":\"10:00:00\",\"horaFim\":\"08:00:00\"}",
            "{\"diaSemana\":\"SEGUNDA\",\"horaInicio\":\"05:00:00\",\"horaFim\":\"07:00:00\"}",
            "{\"diaSemana\":\"SEGUNDA\",\"horaInicio\":\"08:00:00\",\"horaFim\":\"08:10:00\"}",
            "{\"diaSemana\":\"SEGUNDA\",\"horaInicio\":\"08:00:00\",\"horaFim\":\"17:00:00\"}",
            "{\"horaInicio\":\"08:00:00\",\"horaFim\":\"10:00:00\"}",
            "{}", "\"texto\""})
    void horarioEmbutidoInvalido(String horario) throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(disciplinaPadrao("Cálculo I"), "horarios", List.of(new Json.Cru(horario)));
        ResultActions r = post("/disciplinas", Json.escrever(c), s);
        assertSemErro5xx(r, "horario embutido " + horario);
        assertStatusEm(r, "horario embutido " + horario, 400);
    }

    @Test
    void elementoNuloDentroDaListaDeHorarios() throws Exception {
        // POSSIVEL BUG REAL: "horarios":[null] passa pelo @Valid (que ignora nulos) e o service faz h.diaSemana() -> NPE -> 500.
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(disciplinaPadrao("Cálculo I"), "horarios", List.of(new Json.Cru("null")));
        ResultActions r = post("/disciplinas", Json.escrever(c), s);
        assertSemErro5xx(r, "horarios:[null]");
    }

    @Test
    void camposDesconhecidosESensiveisSaoIgnorados() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(Json.com(disciplinaPadrao("Cálculo I"), "arquivada", true), "status", "APROVADO");
        c = Json.com(c, "idUsuario", "00000000-0000-0000-0000-000000000001");
        post("/disciplinas", Json.escrever(c), s).andExpect(status().isCreated())
                .andExpect(jsonPath("$.arquivada").value(false)).andExpect(jsonPath("$.status").value("EM_PROGRESSO"));
    }

    @Test
    void buscarPorIdELista() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        get("/disciplinas/" + id, s).andExpect(status().isOk()).andExpect(jsonPath("$.idDisciplina").value(id));
        get("/disciplinas", s).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        get("/disciplinas/ativas", s).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        get("/disciplinas/arquivadas", s).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listarFiltraPorStatusEPeriodoAtual() throws Exception {
        Sessao s = novaSessao();
        criarDisciplina(s, "Cálculo I");
        criarDisciplina(s, Json.com(disciplinaPadrao("Física I"), "periodo", 5));
        get("/disciplinas", s).andExpect(jsonPath("$.length()").value(1));
        get("/disciplinas?status=EM_PROGRESSO", s).andExpect(jsonPath("$.length()").value(1));
        get("/disciplinas?status=APROVADO", s).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest(name = "status de filtro invalido [{0}]")
    @ValueSource(strings = {"INEXISTENTE", "em_progresso", "1", "'; DROP TABLE disciplina;--", "<script>"})
    void filtroDeStatusInvalidoRetorna400(String status) throws Exception {
        Sessao s = novaSessao();
        ResultActions r = get("/disciplinas?status=" + java.net.URLEncoder.encode(status, java.nio.charset.StandardCharsets.UTF_8), s);
        assertSemErro5xx(r, "filtro status=" + status);
        r.andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "periodo de arquivadas invalido [{0}]")
    @ValueSource(strings = {"abc", "1.5", "'; DROP TABLE disciplina;--"})
    void periodoDeArquivadasInvalido(String p) throws Exception {
        Sessao s = novaSessao();
        ResultActions r = get("/disciplinas/arquivadas?periodo=" + java.net.URLEncoder.encode(p, java.nio.charset.StandardCharsets.UTF_8), s);
        r.andExpect(status().isBadRequest());
    }

    @Test
    void usuarioNaoAcessaDisciplinaDeOutro() throws Exception {
        Sessao dono = novaSessao();
        Sessao intruso = novaSessao();
        String id = criarDisciplina(dono, "Cálculo I");
        get("/disciplinas/" + id, intruso).andExpect(status().isNotFound());
        get("/disciplinas/" + id + "/media", intruso).andExpect(status().isNotFound());
        put("/disciplinas/" + id, Json.escrever(disciplinaPadrao("Invadida")), intruso).andExpect(status().isNotFound());
        patch("/disciplinas/" + id + "/arquivar", null, intruso).andExpect(status().isNotFound());
        patch("/disciplinas/" + id + "/desarquivar", null, intruso).andExpect(status().isNotFound());
        patch("/disciplinas/" + id + "/status", null, intruso).andExpect(status().isNotFound());
        delete("/disciplinas/" + id, null, intruso).andExpect(status().isNotFound());
        get("/disciplinas", intruso).andExpect(jsonPath("$.length()").value(0));
        get("/disciplinas/" + id, dono).andExpect(jsonPath("$.nomeDisciplina").value("Cálculo I"));
    }

    @Test
    void atualizarDisciplina() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        Map<String, Object> novo = Json.com(Json.com(disciplinaPadrao("Cálculo II"), "professor", "Maria Souza"), "cor", "#00AA00");
        put("/disciplinas/" + id, Json.escrever(novo), s).andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeDisciplina").value("Cálculo II")).andExpect(jsonPath("$.professor").value("Maria Souza"))
                .andExpect(jsonPath("$.cor").value("#00AA00"));
    }

    @Test
    void atualizarParaNomeJaUsadoPorOutraDisciplinaDoUsuario() throws Exception {
        Sessao s = novaSessao();
        criarDisciplina(s, "Cálculo I");
        String b = criarDisciplina(s, "Física I");
        put("/disciplinas/" + b, Json.escrever(disciplinaPadrao("Cálculo I")), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("440"));
    }

    @Test
    void atualizarMantendoOProprioNomeNaoEhConflito() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        put("/disciplinas/" + id, Json.escrever(Json.com(disciplinaPadrao("Cálculo I"), "sala", "B202")), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.sala").value("B202"));
    }

    @Test
    void atualizarComCorpoInvalidoNaoAlteraNada() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        put("/disciplinas/" + id, Json.escrever(Json.com(disciplinaPadrao("Cálculo I"), "cor", "vermelho")), s)
                .andExpect(status().isBadRequest());
        put("/disciplinas/" + id, Json.escrever(Json.com(disciplinaPadrao("<script>"), "cor", "#000")), s)
                .andExpect(status().isBadRequest());
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.nomeDisciplina").value("Cálculo I"))
                .andExpect(jsonPath("$.cor").value("#FF5733"));
    }

    @Test
    void tipoDeMediaNaoMudaDepoisDeHaverAvaliacoes() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        criarAvaliacao(s, id);
        put("/disciplinas/" + id, Json.escrever(Json.com(disciplinaPadrao("Cálculo I"), "tipoMedia", "PONDERADA")), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("471"));
    }

    @Test
    void arquivarEDesarquivar() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        patch("/disciplinas/" + id + "/arquivar", null, s).andExpect(status().isNoContent());
        get("/disciplinas/arquivadas", s).andExpect(jsonPath("$.length()").value(1));
        get("/disciplinas/ativas", s).andExpect(jsonPath("$.length()").value(0));
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.arquivada").value(true));
        patch("/disciplinas/" + id + "/desarquivar", null, s).andExpect(status().isOk()).andExpect(jsonPath("$.arquivada").value(false));
        get("/disciplinas/ativas", s).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void desarquivarDisciplinaNaoArquivadaRetorna400Codigo411() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        patch("/disciplinas/" + id + "/desarquivar", null, s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("411"));
    }

    @Test
    void disciplinaArquivadaEhSomenteLeitura() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        patch("/disciplinas/" + id + "/arquivar", null, s).andExpect(status().isNoContent());
        put("/disciplinas/" + id, Json.escrever(disciplinaPadrao("Cálculo I")), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("410"));
        post("/tarefas", Json.escrever(tarefaPadrao(id)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("410"));
        post("/avaliacoes", Json.escrever(avaliacaoPadrao(id)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("410"));
        post("/horarios", Json.escrever(Json.mapa("diaSemana", "SEGUNDA", "horaInicio", "08:00:00", "horaFim", "10:00:00", "idDisciplina", id)), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("410"));
    }

    @Test
    void arquivarDuasVezesNaoGera500() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        patch("/disciplinas/" + id + "/arquivar", null, s).andExpect(status().isNoContent());
        assertSemErro5xx(patch("/disciplinas/" + id + "/arquivar", null, s), "arquivar duas vezes");
    }

    @Test
    void atualizarStatusRecalculaSemErro() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        ResultActions r = patch("/disciplinas/" + id + "/status", null, s);
        assertSemErro5xx(r, "recalcular status");
        r.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_PROGRESSO"));
    }

    @Test
    void excluirDisciplinaVazia() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        delete("/disciplinas/" + id, null, s).andExpect(status().isNoContent());
        get("/disciplinas/" + id, s).andExpect(status().isNotFound());
        delete("/disciplinas/" + id, null, s).andExpect(status().isNotFound());
    }

    @Test
    void excluirDisciplinaComTarefaOuAvaliacaoRetorna400Codigo442() throws Exception {
        Sessao s = novaSessao();
        String comTarefa = criarDisciplina(s, "Cálculo I");
        criarTarefa(s, comTarefa);
        delete("/disciplinas/" + comTarefa, null, s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("442"));
        String comAvaliacao = criarDisciplina(s, "Física I");
        criarAvaliacao(s, comAvaliacao);
        delete("/disciplinas/" + comAvaliacao, null, s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("442"));
        get("/disciplinas/" + comTarefa, s).andExpect(status().isOk());
    }

    @Test
    void excluirDisciplinaApagaOsHorariosDela() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(disciplinaPadrao("Cálculo I"), "horarios", List.of(horarioEmbutido("SEGUNDA", "08:00:00", "10:00:00")));
        String id = criarDisciplina(s, c);
        ResultActions r = delete("/disciplinas/" + id, null, s);
        assertSemErro5xx(r, "excluir disciplina com horarios");
        r.andExpect(status().isNoContent());
        Map<String, Object> outra = Json.com(disciplinaPadrao("Física I"), "horarios", List.of(horarioEmbutido("SEGUNDA", "08:00:00", "10:00:00")));
        criarDisciplina(s, outra);
    }

    @Test
    void mediaSimplesDeDuasAvaliacoesConcluidas() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        String a1 = criarAvaliacao(s, id);
        String a2 = criarAvaliacao(s, id);
        patch("/avaliacoes/" + a1 + "/concluir", Json.obj("notaObtida", 8.0), s).andExpect(status().isOk());
        patch("/avaliacoes/" + a2 + "/concluir", Json.obj("notaObtida", 6.0), s).andExpect(status().isOk());
        ResultActions r = get("/disciplinas/" + id + "/media", s);
        r.andExpect(status().isOk()).andExpect(jsonPath("$.tipoMedia").value("SIMPLES"))
                .andExpect(jsonPath("$.avaliacoes.length()").value(2));
        assertEquals(7.0, ((Number) campo(r, "$.mediaAtual")).doubleValue(), 0.0001);
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void notaEhNormalizadaParaEscalaDez() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        ResultActions c = post("/avaliacoes", Json.escrever(Json.com(avaliacaoPadrao(id), "notaMaxima", 5.0)), s);
        String av = campo(c, "$.idAvaliacao");
        patch("/avaliacoes/" + av + "/concluir", Json.obj("notaObtida", 3.5), s).andExpect(status().isOk());
        assertEquals(7.0, ((Number) campo(get("/disciplinas/" + id + "/media", s), "$.mediaAtual")).doubleValue(), 0.0001);
    }

    @Test
    void disciplinaSemAvaliacoesTemMediaNulaEContinuaEmProgresso() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        ResultActions r = get("/disciplinas/" + id + "/media", s);
        r.andExpect(status().isOk());
        String corpo = r.andReturn().getResponse().getContentAsString();
        assertTrue(!corpo.contains("\"mediaAtual\"") || corpo.contains("\"mediaAtual\":null") || corpo.contains("\"mediaAtual\":0.0"),
                "mediaAtual deveria estar ausente/nula/0 sem avaliações. Corpo: " + corpo);
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.status").value("EM_PROGRESSO"));
    }

    @Test
    void reprovadoQuandoTodasConcluidasEMediaAbaixoDaAprovacao() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        String a1 = criarAvaliacao(s, id);
        patch("/avaliacoes/" + a1 + "/concluir", Json.obj("notaObtida", 3.0), s).andExpect(status().isOk());
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.status").value("REPROVADO"));
    }

    @Test
    void mediaPonderada() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, Json.com(disciplinaPadrao("Cálculo I"), "tipoMedia", "PONDERADA"));
        String a1 = campo(post("/avaliacoes", Json.escrever(Json.com(avaliacaoPadrao(id), "peso", 3.0)), s), "$.idAvaliacao");
        String a2 = campo(post("/avaliacoes", Json.escrever(Json.com(Json.com(avaliacaoPadrao(id), "nomeAvaliacao", "Prova 2"), "peso", 1.0)), s), "$.idAvaliacao");
        patch("/avaliacoes/" + a1 + "/concluir", Json.obj("notaObtida", 8.0), s).andExpect(status().isOk());
        patch("/avaliacoes/" + a2 + "/concluir", Json.obj("notaObtida", 4.0), s).andExpect(status().isOk());
        assertEquals(7.0, ((Number) campo(get("/disciplinas/" + id + "/media", s), "$.mediaAtual")).doubleValue(), 0.0001);
    }
}
