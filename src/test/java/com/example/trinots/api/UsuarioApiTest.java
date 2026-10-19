package com.example.trinots.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /usuarios, GET/PUT /usuarios/me, PUT /usuarios/curso, PATCH /usuarios/senha, media, desativar, excluir. */
class UsuarioApiTest extends ApiTestBase {

    @Test
    void cadastroValido201NaoExpoeSenha() throws Exception {
        String email = emailUnico();
        ResultActions r = cadastrarSemVerificar(email);
        r.andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nome").value("Ana")).andExpect(jsonPath("$.emailVerificado").value(false))
                .andExpect(jsonPath("$.ativo").value(true)).andExpect(jsonPath("$.curso.nomeCurso").value("Ciência da Computação"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.versaoSessao").doesNotExist());
        assertFalse(corpo(r).contains(SENHA));
    }

    @Test
    void cadastroDuplicadoRetorna400Codigo400() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        cadastrarSemVerificar(email).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("400"));
    }

    @Test
    void cadastroNaoPermiteEscalarPrivilegiosPorCamposExtras() throws Exception {
        String email = emailUnico();
        Map<String, Object> corpo = Json.com(Json.com(usuarioPadrao(email), "emailVerificado", true), "ativo", false);
        corpo = Json.com(corpo, "idUsuario", "00000000-0000-0000-0000-000000000001");
        post("/usuarios", Json.escrever(corpo), null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerificado").value(false)).andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.idUsuario").value(org.hamcrest.Matchers.not("00000000-0000-0000-0000-000000000001")));
        post("/auth/login", Json.obj("email", email, "senha", SENHA), null).andExpect(status().isForbidden());
    }

    @Test
    void cadastroCorpoVazioListaTodosOsCamposObrigatorios() throws Exception {
        post("/usuarios", "{}", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("400"))
                .andExpect(jsonPath("$.erros.nome").exists()).andExpect(jsonPath("$.erros.sobrenome").exists())
                .andExpect(jsonPath("$.erros.email").exists()).andExpect(jsonPath("$.erros.senha").exists())
                .andExpect(jsonPath("$.erros.curso").exists()).andExpect(jsonPath("$.erros.semestreAtual").exists());
    }

    @ParameterizedTest(name = "nome vazio/nulo/branco [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void cadastroNomeVazioNuloOuBranco(String nome) throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "nome", nome)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").exists());
    }

    @Test
    void cadastroNomeAusenteDoJson() throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "nome", Json.REMOVER)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").exists());
    }

    @ParameterizedTest(name = "nome com caractere especial/ataque [{0}]")
    @ValueSource(strings = {"Ana1", "Ana@", "Ana#", "Ana$", "Ana%", "Ana*", "Ana_", "Ana.", "Ana;", "<script>alert(1)</script>",
            "' OR '1'='1", "'; DROP TABLE usuario;--", "Robert'); DROP TABLE Students;--", "Ana\nSilva", "Ana\u0000", "😀😀",
            "=cmd|' /C calc'!A0", "A", "../../etc/passwd", "${jndi:ldap://x/a}", "{{7*7}}", "%s%s%s%n"})
    void cadastroNomeComCaracteresInvalidosOuAtaques(String nome) throws Exception {
        ResultActions r = post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "nome", nome)), null);
        r.andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").exists());
    }

    @ParameterizedTest(name = "sobrenome invalido [{0}]")
    @ValueSource(strings = {"", " ", "S1lva", "Silva!", "<b>Silva</b>", "' OR '1'='1", "Silva; DROP TABLE usuario", "😀"})
    void cadastroSobrenomeInvalido(String v) throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "sobrenome", v)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.sobrenome").exists());
    }

    @ParameterizedTest(name = "nome aceito (acentos/apostrofo/hifen) [{0}]")
    @ValueSource(strings = {"José", "João Pedro", "D'Ávila", "Maria-Clara", "Çelina", "Zoë"})
    void cadastroNomesLegitimosComAcentoApostrofoHifen(String nome) throws Exception {
        Map<String, Object> c = Json.com(Json.com(usuarioPadrao(emailUnico()), "nome", nome), "sobrenome", nome);
        post("/usuarios", Json.escrever(c), null).andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value(nome));
    }

    @ParameterizedTest(name = "nome aceito mas inerte: SQL so com letras [{0}]")
    @ValueSource(strings = {"admin'--", "Maria' OR 'a' OR '"})
    void cadastroNomeComSqlSoComLetrasEhGravadoLiteralmenteESemEfeito(String nome) throws Exception {
        Map<String, Object> c = Json.com(usuarioPadrao(emailUnico()), "nome", nome);
        post("/usuarios", Json.escrever(c), null).andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value(nome));
        novaSessao();
    }

    @ParameterizedTest(name = "email invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "semarroba", "@exemplo.com", "ana@", "ana silva@exemplo.com", "ana@@exemplo.com",
            "' OR '1'='1", "<script>@x.com"})
    void cadastroEmailInvalido(String email) throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao("x@x.com"), "email", email)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.email").exists());
    }

    @Test
    void cadastroEmailMuitoLongo() throws Exception {
        String longo = "a".repeat(64) + "@" + "d".repeat(63) + "." + "d".repeat(63) + ".com";
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao("x@x.com"), "email", longo)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.email").exists());
    }

    @Test
    void cadastroEmailComMaiusculasEEspacosTodosOsCenarios() throws Exception {
        String base = emailUnico();
        cadastrarSemVerificar(base).andExpect(status().isCreated());
        ResultActions r = cadastrarSemVerificar(base.toUpperCase());
        assertSemErro5xx(r, "cadastro com e-mail em maiusculas");
        assertStatusEm(r, "cadastro com e-mail em maiusculas", 201, 400);
    }

    @ParameterizedTest(name = "senha fora do limite (len {0})")
    @ValueSource(ints = {0, 1, 3, 33, 100})
    void cadastroSenhaForaDoLimite(int n) throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "senha", "x".repeat(n))), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    void cadastroSenhaNulaOuAusente() throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "senha", null)), null).andExpect(status().isBadRequest());
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "senha", Json.REMOVER)), null).andExpect(status().isBadRequest());
    }

    @Test
    void senhaEhGravadaComHashENuncaEmTextoPuro() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String r = corpo(cadastrarSemVerificar(emailUnico()));
        assertFalse(r.contains(SENHA));
    }

    @ParameterizedTest(name = "semestre invalido [{0}]")
    @ValueSource(strings = {"0", "-1", "21", "999999999999", "\"abc\"", "[]", "{}", "null"})
    void cadastroSemestreInvalido(String valor) throws Exception {
        String corpo = Json.escrever(Json.com(usuarioPadrao(emailUnico()), "semestreAtual", new Json.Cru(valor)));
        post("/usuarios", corpo, null).andExpect(status().isBadRequest());
    }

    @Test
    void cadastroCursoNuloOuInvalido() throws Exception {
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "curso", null)), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.curso").exists());
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "curso", Json.mapa())), null)
                .andExpect(status().isBadRequest());
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "curso", "texto")), null)
                .andExpect(status().isBadRequest());
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "curso",
                Json.mapa("nomeCurso", "'; DROP TABLE curso;--", "instituicao", "UFMG", "duracao", 8))), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$['erros']['curso.nomeCurso']").exists());
        post("/usuarios", Json.escrever(Json.com(usuarioPadrao(emailUnico()), "curso",
                Json.mapa("nomeCurso", "Curso", "instituicao", "UFMG", "duracao", 0))), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$['erros']['curso.duracao']").exists());
    }

    @Test
    void perfilDoUsuarioLogado() throws Exception {
        Sessao s = novaSessao();
        get("/usuarios/me", s).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(s.email()))
                .andExpect(jsonPath("$.idUsuario").value(s.idUsuario().toString()))
                .andExpect(jsonPath("$.emailVerificado").value(true)).andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void usuarioNaoVeOPerfilDeOutroPorParametros() throws Exception {
        Sessao a = novaSessao();
        Sessao b = novaSessao();
        get("/usuarios/me?idUsuario=" + b.idUsuario(), a).andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(a.idUsuario().toString()));
    }

    @Test
    void atualizarPerfil() throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/me", Json.obj("nome", "Maria", "sobrenome", "Souza", "semestreAtual", 5), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.sobrenome").value("Souza")).andExpect(jsonPath("$.semestreAtual").value(5))
                .andExpect(jsonPath("$.email").value(s.email()));
        get("/usuarios/me", s).andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    void atualizarPerfilNaoPermiteMudarEmailIdOuVerificacao() throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/me", Json.obj("nome", "Maria", "sobrenome", "Souza", "semestreAtual", 5, "email", "invasor@x.com",
                "emailVerificado", false, "ativo", false, "idUsuario", "00000000-0000-0000-0000-000000000001"), s)
                .andExpect(status().isOk());
        get("/usuarios/me", s).andExpect(jsonPath("$.email").value(s.email())).andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.emailVerificado").value(true)).andExpect(jsonPath("$.idUsuario").value(s.idUsuario().toString()));
    }

    @ParameterizedTest(name = "atualizar nome invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "Ana1", "<script>", "' OR '1'='1", "'; DROP TABLE usuario;--", "Ana\u0000", "😀"})
    void atualizarNomeInvalido(String nome) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/me", Json.obj("nome", nome, "sobrenome", "Souza", "semestreAtual", 5), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nome").exists());
        get("/usuarios/me", s).andExpect(jsonPath("$.nome").value("Ana"));
    }

    @ParameterizedTest(name = "atualizar semestre invalido [{0}]")
    @ValueSource(strings = {"0", "-1", "21", "null", "\"cinco\""})
    void atualizarSemestreInvalido(String v) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/me", "{\"nome\":\"Ana\",\"sobrenome\":\"Silva\",\"semestreAtual\":" + v + "}", s)
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarCurso() throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/curso", Json.obj("nomeCurso", "Engenharia de Software", "instituicao", "PUC Minas", "duracao", 10), s)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nomeCurso").value("Engenharia de Software"))
                .andExpect(jsonPath("$.duracao").value(10));
        get("/usuarios/me", s).andExpect(jsonPath("$.curso.instituicao").value("PUC Minas"));
    }

    @ParameterizedTest(name = "curso: nome invalido [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", "-Curso", "Curso;", "<script>alert(1)</script>", "' OR '1'='1", "'; DROP TABLE curso;--", "Curso\u0000", "😀", "=1+1"})
    void atualizarCursoNomeInvalido(String nome) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/curso", Json.obj("nomeCurso", nome, "instituicao", "UFMG", "duracao", 8), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.nomeCurso").exists());
    }

    @ParameterizedTest(name = "curso: instituicao invalida [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "U", "UFMG;", "<b>UFMG</b>", "' OR '1'='1", "UFMG\n"})
    void atualizarCursoInstituicaoInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/curso", Json.obj("nomeCurso", "Curso", "instituicao", v, "duracao", 8), s)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.instituicao").exists());
    }

    @ParameterizedTest(name = "curso: duracao invalida [{0}]")
    @ValueSource(strings = {"0", "-1", "21", "null", "\"oito\""})
    void atualizarCursoDuracaoInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/curso", "{\"nomeCurso\":\"Curso\",\"instituicao\":\"UFMG\",\"duracao\":" + v + "}", s)
                .andExpect(status().isBadRequest());
    }

    @Test
    void trocarSenha() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", "novaSenha9", "novaSenhaConfirmada", "novaSenha9"), s)
                .andExpect(status().isNoContent());
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isUnauthorized());
        login(s.email(), "novaSenha9");
    }

    @Test
    void trocarSenhaComSenhaAtualErradaRetorna401() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/senha", Json.obj("senhaAtual", "errada123", "novaSenha", "novaSenha9", "novaSenhaConfirmada", "novaSenha9"), s)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("401"));
        login(s.email(), SENHA);
    }

    @Test
    void trocarSenhaConfirmacaoDiferente() throws Exception {
        Sessao s = novaSessao();
        ResultActions r = patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", "novaSenha9", "novaSenhaConfirmada", "novaSenha8"), s);
        assertSemErro5xx(r, "confirmacao diferente");
        assertStatusEm(r, "confirmacao diferente", 400, 401);
        login(s.email(), SENHA);
    }

    @Test
    void trocarSenhaValidacoes() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/senha", "{}", s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senhaAtual").exists()).andExpect(jsonPath("$.erros.novaSenha").exists())
                .andExpect(jsonPath("$.erros.novaSenhaConfirmada").exists());
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", "abc", "novaSenhaConfirmada", "abc"), s)
                .andExpect(status().isBadRequest());
        patch("/usuarios/senha", Json.obj("senhaAtual", SENHA, "novaSenha", "x".repeat(33), "novaSenhaConfirmada", "x".repeat(33)), s)
                .andExpect(status().isBadRequest());
        patch("/usuarios/senha", Json.obj("senhaAtual", "", "novaSenha", "novaSenha9", "novaSenhaConfirmada", "novaSenha9"), s)
                .andExpect(status().isBadRequest());
        patch("/usuarios/senha", Json.obj("senhaAtual", "' OR '1'='1", "novaSenha", "novaSenha9", "novaSenhaConfirmada", "novaSenha9"), s)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mediaDeAprovacaoPadraoEAlteracao() throws Exception {
        Sessao s = novaSessao();
        get("/usuarios/configuracoes/media", s).andExpect(status().isOk());
        put("/usuarios/configuracoes/media", Json.obj("novaMedia", 7.5), s).andExpect(status().isNoContent());
        assertEquals(7.5, ((Number) campo(get("/usuarios/configuracoes/media", s), "$")).doubleValue());
    }

    @ParameterizedTest(name = "media invalida [{0}]")
    @ValueSource(strings = {"-0.1", "-1", "100.1", "1000", "null", "\"sete\"", "[]"})
    void mediaDeAprovacaoInvalida(String v) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/configuracoes/media", "{\"novaMedia\":" + v + "}", s).andExpect(status().isBadRequest());
    }

    @Test
    void mediaDeAprovacaoSemCampo() throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/configuracoes/media", "{}", s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.novaMedia").exists());
    }

    @ParameterizedTest(name = "media limite aceita [{0}]")
    @ValueSource(doubles = {0.0, 10.0, 100.0})
    void mediaDeAprovacaoNosLimitesDoDto(double v) throws Exception {
        Sessao s = novaSessao();
        put("/usuarios/configuracoes/media", Json.obj("novaMedia", v), s).andExpect(status().isNoContent());
    }

    @Test
    void desativarContaInvalidaOAcessoEBloqueiaLogin() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/me/desativar", null, s).andExpect(status().isNoContent());
        get("/usuarios/me", s).andExpect(status().isUnauthorized());
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isForbidden());
    }

    @Test
    void desativarNaoApagaOsDados() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        patch("/usuarios/me/desativar", null, s).andExpect(status().isNoContent());
        post("/auth/reativar", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isNoContent());
        Sessao de_novo = login(s.email(), SENHA);
        get("/disciplinas/" + disc, de_novo).andExpect(status().isOk());
    }

    @Test
    void excluirContaExigeSenha() throws Exception {
        Sessao s = novaSessao();
        delete("/usuarios/me", "{}", s).andExpect(status().isBadRequest());
        delete("/usuarios/me", Json.obj("senha", ""), s).andExpect(status().isBadRequest());
        delete("/usuarios/me", Json.obj("senha", null), s).andExpect(status().isBadRequest());
        delete("/usuarios/me", Json.obj("senha", "errada123"), s).andExpect(status().isUnauthorized());
        delete("/usuarios/me", Json.obj("senha", "' OR '1'='1"), s).andExpect(status().isUnauthorized());
        get("/usuarios/me", s).andExpect(status().isOk());
    }

    @Test
    void excluirContaApagaTudoEmCascataSemErroDeIntegridade() throws Exception {
        Sessao s = novaSessao();
        String disc = criarDisciplina(s, "Cálculo I");
        criarTarefa(s, disc);
        criarAvaliacao(s, disc);
        post("/horarios", Json.escrever(Json.mapa("diaSemana", "SEGUNDA", "horaInicio", "08:00:00", "horaFim", "10:00:00",
                "idDisciplina", disc)), s).andExpect(status().isCreated());
        delete("/usuarios/me", Json.obj("senha", SENHA), s).andExpect(status().isNoContent());
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isUnauthorized());
        get("/usuarios/me", s).andExpect(status().isUnauthorized());
    }

    @Test
    void excluirContaNaoAfetaOutroUsuario() throws Exception {
        Sessao a = novaSessao();
        Sessao b = novaSessao();
        String discB = criarDisciplina(b, "Física I");
        delete("/usuarios/me", Json.obj("senha", SENHA), a).andExpect(status().isNoContent());
        get("/disciplinas/" + discB, b).andExpect(status().isOk());
    }
}
