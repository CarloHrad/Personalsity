package com.example.trinots.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /horarios: POST, GET /{id}, GET /disciplina/{id}, PUT /{id}, DELETE /{id}. */
class HorarioApiTest extends ApiTestBase {

    private static Map<String, Object> horario(String dia, String ini, String fim, String idDisc) {
        return Json.mapa("diaSemana", dia, "horaInicio", ini, "horaFim", fim, "idDisciplina", idDisc);
    }

    private String criarHorario(Sessao s, String dia, String ini, String fim, String idDisc) throws Exception {
        ResultActions r = post("/horarios", Json.escrever(horario(dia, ini, fim, idDisc)), s);
        r.andExpect(status().isCreated());
        return campo(r, "$.idHora");
    }

    @Test
    void criarBuscarListarAtualizarExcluir() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String id = criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", disc);
        get("/horarios/" + id, s).andExpect(status().isOk()).andExpect(jsonPath("$.diaSemana").value("SEGUNDA"))
                .andExpect(jsonPath("$.horaInicio").value("08:00:00")).andExpect(jsonPath("$.horaFim").value("10:00:00"));
        get("/horarios/disciplina/" + disc, s).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        put("/horarios/" + id, Json.escrever(horario("TERCA", "09:00:00", "11:00:00", disc)), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.diaSemana").value("TERCA"));
        delete("/horarios/" + id, null, s).andExpect(status().isNoContent());
        get("/horarios/" + id, s).andExpect(status().isNotFound());
    }

    @Test
    void camposObrigatorios() throws Exception {
        Sessao s = novaSessao();
        post("/horarios", "{}", s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.diaSemana").exists()).andExpect(jsonPath("$.erros.horaInicio").exists())
                .andExpect(jsonPath("$.erros.horaFim").exists()).andExpect(jsonPath("$.erros.idDisciplina").exists());
    }

    @ParameterizedTest(name = "horario invalido #{index}")
    @ValueSource(strings = {
            "08:00:00|08:00:00",
            "10:00:00|09:00:00",
            "05:59:00|07:00:00",
            "22:00:00|23:59:01",
            "00:00:00|01:00:00",
            "08:00:00|08:14:00",
            "08:00:00|16:01:00",
            "06:00:00|23:59:00"})
    void horarioForaDasRegras(String par) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String[] p = par.split("\\|");
        post("/horarios", Json.escrever(horario("SEGUNDA", p[0], p[1], disc)), s).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "horario valido nos limites #{index}")
    @ValueSource(strings = {"06:00:00|06:15:00", "08:00:00|16:00:00", "22:00:00|23:59:00"})
    void horarioNosLimites(String par) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        String[] p = par.split("\\|");
        post("/horarios", Json.escrever(horario("SEXTA", p[0], p[1], disc)), s).andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "dia/hora com formato invalido [{0}]")
    @ValueSource(strings = {"\"FERIADO\"", "\"segunda\"", "\"\"", "null", "[]", "{}", "\"SEGUNDA; DROP TABLE horario\"", "\"<script>\""})
    void diaDaSemanaInvalido(String dia) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/horarios", Json.escrever(Json.com(horario("SEGUNDA", "08:00:00", "10:00:00", disc), "diaSemana", new Json.Cru(dia))), s)
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "hora ilegivel [{0}]")
    @ValueSource(strings = {"\"25:00:00\"", "\"08:60:00\"", "\"oito\"", "\"\"", "\"8h\"", "\"08:00:00; DROP TABLE x\"", "[]", "{}", "true"})
    void horaIlegivel(String hora) throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/horarios", Json.escrever(Json.com(horario("SEGUNDA", "08:00:00", "10:00:00", disc), "horaInicio", new Json.Cru(hora))), s);
        assertSemErro5xx(r, "horaInicio=" + hora);
        r.andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "idDisciplina invalido [{0}]")
    @ValueSource(strings = {"\"abc\"", "\"\"", "123", "null", "\"' OR '1'='1\"", "\"11111111-1111-1111-1111-11111111111\"", "[]"})
    void idDisciplinaInvalido(String id) throws Exception {
        Sessao s = novaSessao();
        post("/horarios", Json.escrever(Json.com(horario("SEGUNDA", "08:00:00", "10:00:00", "x"), "idDisciplina", new Json.Cru(id))), s)
                .andExpect(status().isBadRequest());
    }

    @Test
    void disciplinaInexistente404() throws Exception {
        Sessao s = novaSessao();
        post("/horarios", Json.escrever(horario("SEGUNDA", "08:00:00", "10:00:00", UUID_INEXISTENTE)), s).andExpect(status().isNotFound());
    }

    @Test
    void conflitoNaMesmaDisciplinaEEntreDisciplinasDoMesmoPeriodo() throws Exception {
        Sessao s = novaSessao();
        String a = criarDisciplina(s, "Cálculo I");
        String b = criarDisciplina(s, "Física I");
        criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", a);
        post("/horarios", Json.escrever(horario("SEGUNDA", "09:00:00", "11:00:00", a)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("450"));
        post("/horarios", Json.escrever(horario("SEGUNDA", "09:00:00", "11:00:00", b)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("450"));
        post("/horarios", Json.escrever(horario("SEGUNDA", "07:00:00", "08:30:00", b)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("450"));
        post("/horarios", Json.escrever(horario("SEGUNDA", "08:30:00", "09:30:00", b)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("450"));
        post("/horarios", Json.escrever(horario("SEGUNDA", "06:00:00", "12:00:00", b)), s).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("450"));
    }

    @Test
    void horariosEncostadosOuEmOutroDiaNaoConflitam() throws Exception {
        Sessao s = novaSessao();
        String a = criarDisciplina(s, "Cálculo I");
        String b = criarDisciplina(s, "Física I");
        criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", a);
        criarHorario(s, "SEGUNDA", "10:00:00", "12:00:00", b);
        criarHorario(s, "TERCA", "08:00:00", "10:00:00", b);
    }

    @Test
    void disciplinasDePeriodosDiferentesNaoConflitam() throws Exception {
        Sessao s = novaSessao();
        String a = criarDisciplina(s, "Cálculo I");
        String b = criarDisciplina(s, Json.com(disciplinaPadrao("Física I"), "periodo", 5));
        criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", a);
        criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", b);
    }

    @Test
    void horarioNaoConflitaComSiMesmoAoSerAtualizado() throws Exception {
        Sessao s = novaSessao();
        String a = criarDisciplina(s, "Cálculo I");
        String id = criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", a);
        put("/horarios/" + id, Json.escrever(horario("SEGUNDA", "08:30:00", "10:30:00", a)), s).andExpect(status().isOk());
    }

    @Test
    void atualizarParaConflitoRetorna450EMantemOHorarioOriginal() throws Exception {
        Sessao s = novaSessao();
        String a = criarDisciplina(s, "Cálculo I");
        String b = criarDisciplina(s, "Física I");
        criarHorario(s, "SEGUNDA", "08:00:00", "10:00:00", a);
        String id = criarHorario(s, "TERCA", "08:00:00", "10:00:00", b);
        put("/horarios/" + id, Json.escrever(horario("SEGUNDA", "09:00:00", "10:00:00", b)), s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("450"));
        get("/horarios/" + id, s).andExpect(jsonPath("$.diaSemana").value("TERCA"));
    }

    @Test
    void isolamentoEntreUsuarios() throws Exception {
        Sessao dono = novaSessao();
        Sessao intruso = novaSessao();
        String disc = criarDisciplina(dono, "Cálculo I");
        String id = criarHorario(dono, "SEGUNDA", "08:00:00", "10:00:00", disc);
        String discIntruso = criarDisciplina(intruso, "Física I");
        get("/horarios/" + id, intruso).andExpect(status().isNotFound());
        get("/horarios/disciplina/" + disc, intruso).andExpect(status().isNotFound());
        put("/horarios/" + id, Json.escrever(horario("SEXTA", "08:00:00", "09:00:00", disc)), intruso).andExpect(status().isNotFound());
        put("/horarios/" + id, Json.escrever(horario("SEXTA", "08:00:00", "09:00:00", discIntruso)), intruso).andExpect(status().isNotFound());
        delete("/horarios/" + id, null, intruso).andExpect(status().isNotFound());
        post("/horarios", Json.escrever(horario("SEXTA", "08:00:00", "09:00:00", disc)), intruso).andExpect(status().isNotFound());
        get("/horarios/" + id, dono).andExpect(jsonPath("$.diaSemana").value("SEGUNDA"));
    }

    @Test
    void naoMoveHorarioDeUmUsuarioParaDisciplinaDeOutro() throws Exception {
        Sessao dono = novaSessao();
        Sessao outro = novaSessao();
        String disc = criarDisciplina(dono, "Cálculo I");
        String id = criarHorario(dono, "SEGUNDA", "08:00:00", "10:00:00", disc);
        String discOutro = criarDisciplina(outro, "Física I");
        ResultActions r = put("/horarios/" + id, Json.escrever(horario("SEGUNDA", "08:00:00", "10:00:00", discOutro)), dono);
        r.andExpect(status().isNotFound());
        get("/horarios/disciplina/" + discOutro, outro).andExpect(jsonPath("$.length()").value(0));
    }
}
