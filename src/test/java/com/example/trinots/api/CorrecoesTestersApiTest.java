package com.example.trinots.api;

import com.auth0.jwt.JWT;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorrecoesTestersApiTest extends ApiTestBase {

    private static final String SENHA_NOVA = "novaSenha9";

    @Test
    void cadastroComSemestreMaiorQueDuracaoRetorna400() throws Exception {
        String email = emailUnico();
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(email), "semestreAtual", 9)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("476"));
        post("/usuarios", Json.escrever(usuarioPadrao(email)), null).andExpect(status().isCreated());
    }

    @Test
    void cadastroComSemestreIgualADuracaoEhAceito() throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "semestreAtual", 8)), null)
                .andExpect(status().isCreated());
    }

    @Test
    void atualizarPerfilComSemestreMaiorQueDuracaoRetorna400() throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/me", Json.obj("nome", "Ana", "sobrenome", "Silva", "semestreAtual", 9), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("476"));
        put("/usuarios/me", Json.obj("nome", "Ana", "sobrenome", "Silva", "semestreAtual", 8), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.semestreAtual").value(8));
    }

    @Test
    void reduzirDuracaoDoCursoAbaixoDoSemestreAtualRetorna400() throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/curso", Json.obj("nomeCurso", "Curso", "instituicao", "UFMG", "duracao", 2), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("476"));
        get("/usuarios/me", s).andExpect(jsonPath("$.curso.duracao").value(8));
        put("/usuarios/curso", Json.obj("nomeCurso", "Curso", "instituicao", "UFMG", "duracao", 3), s)
                .andExpect(status().isOk());
    }

    @Test
    void trocarParaAMesmaSenhaRetorna400Codigo477() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", SENHA, "novaSenhaConfirmada", SENHA), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("477"));
        login(s.email(), SENHA);
    }

    @Test
    void trocarSenhaValidaContinuaFuncionando() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", SENHA_NOVA, "novaSenhaConfirmada", SENHA_NOVA), s)
                .andExpect(status().isNoContent());
        login(s.email(), SENHA_NOVA);
    }

    @Test
    void loginComSenhaErradaContinuaRetornando401() throws Exception {
        Sessao s = novaSessao();
        post("/auth/login", Json.obj("email", s.email(), "senha", "errada123"), null).andExpect(status().isUnauthorized());
    }

    @Test
    void disciplinaSemProfessorENemSalaEhCriada() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(Json.com(disciplinaPadrao("Cálculo I"), "professor", Json.REMOVER), "sala", Json.REMOVER);
        ResultActions r = post("/disciplinas", Json.escrever(c), s);
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.professor").doesNotExist())
                .andExpect(jsonPath("$.sala").doesNotExist());
    }

    @Test
    void professorESalaVaziosViramAusentes() throws Exception {
        Sessao s = novaSessao();
        Map<String, Object> c = Json.com(Json.com(disciplinaPadrao("Cálculo I"), "professor", ""), "sala", "");
        String id = criarDisciplina(s, c);
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.professor").doesNotExist()).andExpect(jsonPath("$.sala").doesNotExist());
    }

    @Test
    void editarDisciplinaPodeRemoverProfessorESala() throws Exception {
        Sessao s = novaSessao();
        String id = criarDisciplina(s, "Cálculo I");
        put("/disciplinas/" + id, Json.escrever(Json.com(Json.com(disciplinaPadrao("Cálculo I"), "professor", ""), "sala", "")), s)
                .andExpect(status().isOk());
        get("/disciplinas/" + id, s).andExpect(jsonPath("$.professor").doesNotExist()).andExpect(jsonPath("$.sala").doesNotExist());
    }

    @Test
    void nomeContinuaObrigatorio() throws Exception {
        Sessao s = novaSessao();
        post("/disciplinas", Json.escrever(Json.com(disciplinaPadrao("x"), "nomeDisciplina", "")), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nomeDisciplina").exists());
    }

    @Test
    void avaliacaoComDataPassadaEPendenteEstaAguardandoNota() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        ResultActions r = post("/avaliacoes", Json.escrever(Json.com(avaliacaoPadrao(disc), "dataAvaliacao", "2020-01-10")), s);
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.aguardandoNota").value(true))
                .andExpect(jsonPath("$.concluida").value(false));
        String id = campo(r, "$.idAvaliacao");

        patch("/avaliacoes/" + id + "/concluir", Json.obj("notaObtida", 7.5), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.aguardandoNota").value(false));
        patch("/avaliacoes/" + id + "/reabrir", null, s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.aguardandoNota").value(true));
    }

    @Test
    void avaliacaoFuturaNaoEstaAguardandoNota() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(avaliacaoPadrao(disc)), s)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.aguardandoNota").value(false));
    }

    @Test
    void aguardandoNotaApareceNaListagemPorDisciplina() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        post("/avaliacoes", Json.escrever(Json.com(avaliacaoPadrao(disc), "dataAvaliacao", "2020-01-10")), s).andExpect(status().isCreated());
        get("/avaliacoes/disciplina/" + disc, s).andExpect(status().isOk()).andExpect(jsonPath("$[0].aguardandoNota").value(true));
    }

    @Test
    void tokenValeVariosDiasPorPadrao() throws Exception {
        Sessao s = novaSessao();
        String jwt = s.authorization().substring("Bearer ".length());
        Instant expira = JWT.decode(jwt).getExpiresAtAsInstant();
        Duration restante = Duration.between(Instant.now(), expira);
        assertTrue(restante.toHours() >= 160 && restante.toHours() <= 169,
                "esperado ~168h (7 dias), veio " + restante.toHours() + "h");
    }

    @Test
    void tokensAntigosDeixamDeValerAposTrocarSenha() throws Exception {
        Sessao s = novaSessao();
        Sessao outroAparelho = login(s.email(), SENHA);
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", SENHA_NOVA, "novaSenhaConfirmada", SENHA_NOVA), s)
                .andExpect(status().isNoContent());
        get("/usuarios/me", s).andExpect(status().isUnauthorized());
        get("/usuarios/me", outroAparelho).andExpect(status().isUnauthorized());
        Sessao nova = login(s.email(), SENHA_NOVA);
        get("/usuarios/me", nova).andExpect(status().isOk());
    }

    @Test
    void tokensAntigosDeixamDeValerAposRedefinirSenha() throws Exception {
        Sessao antes = novaSessao();
        post("/auth/esqueci-senha", Json.obj("email", antes.email()), null).andExpect(status().isNoContent());
        String codigo = ultimoCodigo(antes.email(), TipoCodigoEnum.RECUPERACAO_SENHA);
        post("/auth/redefinir-senha", Json.obj("email", antes.email(), "codigo", codigo, "novaSenha", SENHA_NOVA), null)
                .andExpect(status().isNoContent());
        get("/usuarios/me", antes).andExpect(status().isUnauthorized());
        get("/usuarios/me", login(antes.email(), SENHA_NOVA)).andExpect(status().isOk());
    }

    @Test
    void trocarSenhaFalhaNaoDerrubaAsSessoes() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/senha", Json.obj("senhaAtual", "errada123", "novaSenha", SENHA_NOVA, "novaSenhaConfirmada", SENHA_NOVA), s)
                .andExpect(status().isUnauthorized());
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", SENHA, "novaSenhaConfirmada", SENHA), s)
                .andExpect(status().isBadRequest());
        get("/usuarios/me", s).andExpect(status().isOk());
    }

    @Test
    void tokenAnteriorADesativacaoNaoVoltaAValerAoReativar() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/me/desativar", null, s).andExpect(status().isNoContent());
        post("/auth/reativar", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isNoContent());
        get("/usuarios/me", s).andExpect(status().isUnauthorized());
        get("/usuarios/me", login(s.email(), SENHA)).andExpect(status().isOk());
    }
}
