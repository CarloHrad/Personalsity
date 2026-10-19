package com.example.trinots.api;

import com.example.trinots.domain.enums.TipoCodigoEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /auth/login, /logout, /verificar-email, /reenviar-verificacao, /esqueci-senha, /redefinir-senha, /reativar. */
class AuthApiTest extends ApiTestBase {

    @Test
    void loginComSucessoDevolveTokenComPrefixoBearerIdENome() throws Exception {
        Sessao s = novaSessao();
        ResultActions r = post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null);
        r.andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.idUsuario").value(s.idUsuario().toString()));
        String token = campo(r, "$.token");
        assertTrue(token.startsWith("Bearer ") && token.length() > 40);
    }

    @Test
    void loginSenhaErrada401() throws Exception {
        Sessao s = novaSessao();
        post("/auth/login", Json.obj("email", s.email(), "senha", "outraSenha"), null)
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("401"));
    }

    @Test
    void loginEmailInexistenteTemMesmaRespostaQueSenhaErrada() throws Exception {
        Sessao s = novaSessao();
        ResultActions inexistente = post("/auth/login", Json.obj("email", emailUnico(), "senha", SENHA), null);
        ResultActions errada = post("/auth/login", Json.obj("email", s.email(), "senha", "outraSenha"), null);
        assertEquals(statusDe(errada), statusDe(inexistente));
        assertEquals(Optional.ofNullable(campo(errada, "$.mensagem")), Optional.ofNullable(campo(inexistente, "$.mensagem")));
    }

    @ParameterizedTest(name = "login com email hostil [{0}]")
    @ValueSource(strings = {"' OR '1'='1", "admin'--@exemplo.com", "'; DROP TABLE usuario;--@x.com", "a@b.com' OR '1'='1",
            "\" OR \"\"=\"@x.com", "<script>alert(1)</script>@x.com", "ana@exemplo.com\r\nBcc: x@y.com"})
    void loginComSqlInjectionNoEmail(String email) throws Exception {
        ResultActions r = post("/auth/login", Json.obj("email", email, "senha", SENHA), null);
        assertSemErro5xx(r, "login email=" + email);
        assertStatusEm(r, "login email=" + email, 400, 401);
    }

    @ParameterizedTest(name = "login com senha hostil [{0}]")
    @ValueSource(strings = {"' OR '1'='1", "' OR 1=1 --", "'; DROP TABLE usuario;--", "\" OR \"\"=\"", "senha' --"})
    void loginComSqlInjectionNaSenhaNaoAutentica(String senha) throws Exception {
        Sessao s = novaSessao();
        ResultActions r = post("/auth/login", Json.obj("email", s.email(), "senha", senha), null);
        assertSemErro5xx(r, "login senha=" + senha);
        assertStatusEm(r, "login senha=" + senha, 400, 401);
        login(s.email(), SENHA);
    }

    @Test
    void loginCamposObrigatorios() throws Exception {
        post("/auth/login", "{}", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists()).andExpect(jsonPath("$.erros.senha").exists());
        post("/auth/login", Json.obj("email", null, "senha", null), null).andExpect(status().isBadRequest());
        post("/auth/login", Json.obj("email", "", "senha", ""), null).andExpect(status().isBadRequest());
        post("/auth/login", Json.obj("email", "   ", "senha", "   "), null).andExpect(status().isBadRequest());
        post("/auth/login", Json.obj("email", "semarroba", "senha", SENHA), null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists());
        post("/auth/login", Json.obj("email", "a@b.com", "senha", "abc"), null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    void loginComTiposErrados() throws Exception {
        post("/auth/login", "{\"email\":123,\"senha\":true}", null).andExpect(status().isBadRequest());
        post("/auth/login", "{\"email\":[\"a@b.com\"],\"senha\":{}}", null).andExpect(status().isBadRequest());
        post("/auth/login", "{\"email\":{\"$ne\":null},\"senha\":{\"$ne\":null}}", null).andExpect(status().isBadRequest());
    }

    @Test
    void loginIgnoraCamposDesconhecidos() throws Exception {
        Sessao s = novaSessao();
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA, "admin", true, "ativo", true), null)
                .andExpect(status().isOk());
    }

    @Test
    void loginSemVerificarEmail403Codigo480() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        post("/auth/login", Json.obj("email", email, "senha", SENHA), null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("480"));
    }

    @Test
    void loginComContaDesativada403Codigo481() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/me/desativar", null, s).andExpect(status().isNoContent());
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("481"));
    }

    @Test
    void emailComCaixaDiferenteNoLogin() throws Exception {
        Sessao s = novaSessao();
        ResultActions r = post("/auth/login", Json.obj("email", s.email().toUpperCase(), "senha", SENHA), null);
        assertSemErro5xx(r, "login com e-mail em maiusculas");
        assertStatusEm(r, "login com e-mail em maiusculas", 200, 401);
    }

    @Test
    void logoutInvalidaOTokenImediatamente() throws Exception {
        Sessao s = novaSessao();
        get("/usuarios/me", s).andExpect(status().isOk());
        post("/auth/logout", null, s).andExpect(status().isNoContent());
        get("/usuarios/me", s).andExpect(status().isUnauthorized());
        post("/auth/logout", null, s).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevogaSoOTokenUsado() throws Exception {
        Sessao s = novaSessao();
        Sessao outroDispositivo = login(s.email(), SENHA);
        assertNotEquals(s.authorization(), outroDispositivo.authorization());
        post("/auth/logout", null, s).andExpect(status().isNoContent());
        get("/usuarios/me", outroDispositivo).andExpect(status().isOk());
    }

    @Test
    void verificarEmailComCodigoCorreto() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String codigo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        assertTrue(codigo.matches("\\d{6}"));
        post("/auth/verificar-email", Json.obj("email", email, "codigo", codigo), null).andExpect(status().isNoContent());
        login(email, SENHA);
    }

    @Test
    void codigoDeVerificacaoEhDeUsoUnico() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String codigo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        post("/auth/verificar-email", Json.obj("email", email, "codigo", codigo), null).andExpect(status().isNoContent());
        ResultActions segunda = post("/auth/verificar-email", Json.obj("email", email, "codigo", codigo), null);
        segunda.andExpect(status().isBadRequest());
    }

    @Test
    void codigoErradoRetorna400Codigo430() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String certo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        String errado = certo.equals("000000") ? "111111" : "000000";
        post("/auth/verificar-email", Json.obj("email", email, "codigo", errado), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("430"));
    }

    @Test
    void cincoTentativasErradasBloqueiamOCodigoMesmoQueOProximoSejaCorreto() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String certo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        String errado = certo.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            post("/auth/verificar-email", Json.obj("email", email, "codigo", errado), null).andExpect(status().isBadRequest());
        }
        post("/auth/verificar-email", Json.obj("email", email, "codigo", certo), null)
                .andExpect(status().isBadRequest());
        post("/auth/login", Json.obj("email", email, "senha", SENHA), null).andExpect(status().isForbidden());
    }

    @Test
    void reenviarVerificacaoGeraNovoCodigoEInvalidaOAnterior() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String antigo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        post("/auth/reenviar-verificacao", Json.obj("email", email), null).andExpect(status().isNoContent());
        String novo = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        if (!antigo.equals(novo)) {
            post("/auth/verificar-email", Json.obj("email", email, "codigo", antigo), null).andExpect(status().isBadRequest());
        }
        post("/auth/verificar-email", Json.obj("email", email, "codigo", novo), null).andExpect(status().isNoContent());
    }

    @Test
    void reenviarParaEmailInexistenteNaoRevelaSeExiste() throws Exception {
        post("/auth/reenviar-verificacao", Json.obj("email", emailUnico()), null).andExpect(status().isNoContent());
    }

    @Test
    void verificarEmailDeUsuarioInexistente() throws Exception {
        ResultActions r = post("/auth/verificar-email", Json.obj("email", emailUnico(), "codigo", "123456"), null);
        assertSemErro5xx(r, "verificar email inexistente");
        assertStatusEm(r, "verificar email inexistente", 400);
    }

    @ParameterizedTest(name = "verificar-email com codigo invalido [{0}]")
    @ValueSource(strings = {"", " ", "12345", "1234567", "abcdef", "12 456", "' OR '1'='1", "１２３４５６", "123456;--", "000000\n"})
    void verificarEmailComCodigoMalformado400(String codigo) throws Exception {
        post("/auth/verificar-email", Json.obj("email", emailUnico(), "codigo", codigo), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.codigo").exists());
    }

    @Test
    void verificarEmailCamposAusentes() throws Exception {
        post("/auth/verificar-email", "{}", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists()).andExpect(jsonPath("$.erros.codigo").exists());
        post("/auth/reenviar-verificacao", "{}", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists());
    }

    @Test
    void esqueciSenhaRespondeIgualParaEmailExistenteEInexistente() throws Exception {
        Sessao s = novaSessao();
        ResultActions existe = post("/auth/esqueci-senha", Json.obj("email", s.email()), null);
        ResultActions naoExiste = post("/auth/esqueci-senha", Json.obj("email", emailUnico()), null);
        existe.andExpect(status().isNoContent());
        naoExiste.andExpect(status().isNoContent());
        assertEquals("", corpo(existe));
        assertEquals("", corpo(naoExiste));
    }

    @Test
    void esqueciSenhaComEmailInvalido400() throws Exception {
        post("/auth/esqueci-senha", "{}", null).andExpect(status().isBadRequest());
        post("/auth/esqueci-senha", Json.obj("email", "' OR 1=1 --"), null).andExpect(status().isBadRequest());
        post("/auth/esqueci-senha", Json.obj("email", ""), null).andExpect(status().isBadRequest());
        post("/auth/esqueci-senha", Json.obj("email", null), null).andExpect(status().isBadRequest());
    }

    @Test
    void fluxoCompletoDeRecuperacaoDeSenha() throws Exception {
        Sessao s = novaSessao();
        post("/auth/esqueci-senha", Json.obj("email", s.email()), null).andExpect(status().isNoContent());
        String codigo = ultimoCodigo(s.email(), TipoCodigoEnum.RECUPERACAO_SENHA);
        post("/auth/redefinir-senha", Json.obj("email", s.email(), "codigo", codigo, "novaSenha", "novaSenha9"), null)
                .andExpect(status().isNoContent());
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isUnauthorized());
        login(s.email(), "novaSenha9");
    }

    @Test
    void codigoDeRecuperacaoNaoServeDuasVezes() throws Exception {
        Sessao s = novaSessao();
        post("/auth/esqueci-senha", Json.obj("email", s.email()), null).andExpect(status().isNoContent());
        String codigo = ultimoCodigo(s.email(), TipoCodigoEnum.RECUPERACAO_SENHA);
        post("/auth/redefinir-senha", Json.obj("email", s.email(), "codigo", codigo, "novaSenha", "novaSenha9"), null)
                .andExpect(status().isNoContent());
        post("/auth/redefinir-senha", Json.obj("email", s.email(), "codigo", codigo, "novaSenha", "outraSenha8"), null)
                .andExpect(status().isBadRequest());
        login(s.email(), "novaSenha9");
    }

    @Test
    void codigoDeVerificacaoNaoRedefineSenha() throws Exception {
        String email = emailUnico();
        cadastrarSemVerificar(email).andExpect(status().isCreated());
        String codigoVerificacao = ultimoCodigo(email, TipoCodigoEnum.VERIFICACAO_EMAIL);
        ResultActions r = post("/auth/redefinir-senha", Json.obj("email", email, "codigo", codigoVerificacao, "novaSenha", "novaSenha9"), null);
        r.andExpect(status().isBadRequest());
    }

    @Test
    void redefinirSenhaComCodigoErradoNaoAlteraSenha() throws Exception {
        Sessao s = novaSessao();
        post("/auth/esqueci-senha", Json.obj("email", s.email()), null).andExpect(status().isNoContent());
        String certo = ultimoCodigo(s.email(), TipoCodigoEnum.RECUPERACAO_SENHA);
        String errado = certo.equals("000000") ? "111111" : "000000";
        post("/auth/redefinir-senha", Json.obj("email", s.email(), "codigo", errado, "novaSenha", "novaSenha9"), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("430"));
        login(s.email(), SENHA);
    }

    @Test
    void redefinirSenhaValidacoes() throws Exception {
        post("/auth/redefinir-senha", "{}", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists()).andExpect(jsonPath("$.erros.codigo").exists())
                .andExpect(jsonPath("$.erros.novaSenha").exists());
        post("/auth/redefinir-senha", Json.obj("email", "a@b.com", "codigo", "123456", "novaSenha", "abc"), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.novaSenha").exists());
        post("/auth/redefinir-senha", Json.obj("email", "a@b.com", "codigo", "12345", "novaSenha", SENHA), null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.codigo").exists());
        post("/auth/redefinir-senha", Json.obj("email", "a@b.com", "codigo", "123456", "novaSenha", "x".repeat(33)), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void sessoesAntigasAposTrocaOuRedefinicaoDeSenha() throws Exception {
        Sessao antes = novaSessao();
        post("/auth/esqueci-senha", Json.obj("email", antes.email()), null).andExpect(status().isNoContent());
        String codigo = ultimoCodigo(antes.email(), TipoCodigoEnum.RECUPERACAO_SENHA);
        post("/auth/redefinir-senha", Json.obj("email", antes.email(), "codigo", codigo, "novaSenha", "novaSenha9"), null)
                .andExpect(status().isNoContent());
        assertStatusEm(get("/usuarios/me", antes), "token antigo apos redefinir senha", 200, 401);
    }

    @Test
    void reativarContaDesativada() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/me/desativar", null, s).andExpect(status().isNoContent());
        get("/usuarios/me", s).andExpect(status().isUnauthorized());
        post("/auth/reativar", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isNoContent());
        login(s.email(), SENHA);
    }

    @Test
    void reativarComSenhaErradaNaoReativa() throws Exception {
        Sessao s = novaSessao();
        patch("/usuarios/me/desativar", null, s).andExpect(status().isNoContent());
        post("/auth/reativar", Json.obj("email", s.email(), "senha", "errada123"), null).andExpect(status().isUnauthorized());
        post("/auth/login", Json.obj("email", s.email(), "senha", SENHA), null).andExpect(status().isForbidden());
    }

    @Test
    void reativarContaJaAtivaOuInexistenteNaoGera500() throws Exception {
        Sessao s = novaSessao();
        assertSemErro5xx(post("/auth/reativar", Json.obj("email", s.email(), "senha", SENHA), null), "reativar conta ativa");
        assertSemErro5xx(post("/auth/reativar", Json.obj("email", emailUnico(), "senha", SENHA), null), "reativar inexistente");
    }

    @Test
    void reativarValidacoes() throws Exception {
        post("/auth/reativar", "{}", null).andExpect(status().isBadRequest());
        post("/auth/reativar", Json.obj("email", "' OR 1=1 --", "senha", SENHA), null).andExpect(status().isBadRequest());
    }

    @Test
    void emailServiceNaoEhChamadoParaEmailInexistente() throws Exception {
        String email = emailUnico();
        Mockito.clearInvocations(emailService);
        post("/auth/esqueci-senha", Json.obj("email", email), null).andExpect(status().isNoContent());
        Mockito.verifyNoInteractions(emailService);
    }
}
