package com.example.trinots.validacao;

import com.example.trinots.dto.AuthDTO.*;
import com.example.trinots.dto.CursoDTO.CursoRequestDTO;
import com.example.trinots.dto.UsuarioDTO.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

/**
 * POST /usuarios, PUT /usuarios/me, PUT /usuarios/curso, PATCH /usuarios/senha,
 * PUT /usuarios/configuracoes/media, DELETE /usuarios/me e todos os /auth/*.
 */
class UsuarioAuthDtoValidacaoTest extends ValidacaoSuporte {

    private static final CursoRequestDTO CURSO_OK =
            new CursoRequestDTO("Ciência da Computação", "UFMG", 8);

    static UsuarioRequestDTO usuario(String nome, String sobrenome, String email, String senha,
                                     CursoRequestDTO curso, Integer semestre) {
        return new UsuarioRequestDTO(nome, sobrenome, email, senha, curso, semestre);
    }

    static UsuarioRequestDTO usuarioOk() {
        return usuario("Ana", "Silva", "ana@exemplo.com", "senha123", CURSO_OK, 3);
    }

    static Stream<String> nomesPessoaValidos() {
        return Stream.of("Ana", "Al", "José", "João Pedro", "D'Ávila", "Maria-Clara", "Çelina", "Ünal", "Ångström",
                "Zoë", "  Ana", "Ana  ", "A".repeat(100), "admin'--", "' UNION SELECT senha FROM usuario--");
    }

    static Stream<String> nomesPessoaInvalidos() {
        Stream.Builder<String> b = Stream.builder();
        for (String s : new String[]{"A", "J", "A".repeat(101), "João1", "123", "Jo@o", "João_Silva", "João.Silva",
                "João!", "João#", "João$", "João%", "João&Maria", "João*", "João+", "João=", "João/Silva", "João\\Silva",
                "João,Silva", "João;", "João:", "João?", "João(a)", "João[a]", "João{a}", "João|Silva", "João~",
                "😀😀", "Ana😀", "Łukasz", "Nguyễn", "Çağlar", "李雷", "O’Connor", "Jo×ao", "Jo÷ao"}) b.add(s);
        for (String s : Ataques.SQL_COM_SIMBOLOS) b.add(s);
        for (String s : Ataques.XSS) b.add(s);
        for (String s : Ataques.CONTROLE) b.add(s);
        for (String s : Ataques.INJECAO_FORMULA_CSV) b.add(s);
        return b.build();
    }

    @Nested
    class CriarUsuario {
        @Test
        void corpoValidoPassa() {
            assertValido(usuarioOk());
        }

        @ParameterizedTest(name = "nome valido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaValidos")
        void nomeValido(String v) {
            assertValido(usuario(v, "Silva", "ana@exemplo.com", "senha123", CURSO_OK, 3));
        }

        @ParameterizedTest(name = "nome invalido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaInvalidos")
        void nomeInvalido(String v) {
            assertInvalido(usuario(v, "Silva", "ana@exemplo.com", "senha123", CURSO_OK, 3), "nome");
        }

        @ParameterizedTest(name = "nome vazio/nulo/branco [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n", " "})
        void nomeVazioNuloOuBranco(String v) {
            assertInvalido(usuario(v, "Silva", "ana@exemplo.com", "senha123", CURSO_OK, 3), "nome");
        }

        @Test
        void nomeNuloMensagem() {
            assertInvalido(usuario(null, "Silva", "ana@exemplo.com", "senha123", CURSO_OK, 3), "nome", "obrigat");
        }

        @ParameterizedTest(name = "sobrenome valido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaValidos")
        void sobrenomeValido(String v) {
            assertValido(usuario("Ana", v, "ana@exemplo.com", "senha123", CURSO_OK, 3));
        }

        @ParameterizedTest(name = "sobrenome invalido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaInvalidos")
        void sobrenomeInvalido(String v) {
            assertInvalido(usuario("Ana", v, "ana@exemplo.com", "senha123", CURSO_OK, 3), "sobrenome");
        }

        @ParameterizedTest(name = "sobrenome vazio/nulo/branco [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void sobrenomeVazioNuloOuBranco(String v) {
            assertInvalido(usuario("Ana", v, "ana@exemplo.com", "senha123", CURSO_OK, 3), "sobrenome");
        }

        @ParameterizedTest(name = "email valido [{0}]")
        @ValueSource(strings = {"ana@exemplo.com", "ana.silva@exemplo.com.br", "ana+tag@exemplo.com", "a@b.co",
                "ANA@EXEMPLO.COM", "ana_silva@sub.dominio.edu.br"})
        void emailValido(String v) {
            assertValido(usuario("Ana", "Silva", v, "senha123", CURSO_OK, 3));
        }

        @ParameterizedTest(name = "email invalido [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "semarroba", "@exemplo.com", "ana@", "ana silva@exemplo.com", "ana@@exemplo.com",
                "' OR '1'='1", "<script>@x.com", "ana@exemplo.com\nBcc: x@y.com"})
        void emailInvalido(String v) {
            assertInvalido(usuario("Ana", "Silva", v, "senha123", CURSO_OK, 3), "email");
        }

        @Test
        void emailNoLimiteDe150Passa() {
            String email = "ana@" + "d".repeat(60) + "." + "d".repeat(60) + "." + "com";
            org.junit.jupiter.api.Assertions.assertTrue(email.length() <= 150);
            assertValido(usuario("Ana", "Silva", email, "senha123", CURSO_OK, 3));
        }

        @Test
        void emailAcimaDe150Falha() {
            String email = "ana@" + "d".repeat(60) + "." + "d".repeat(60) + "." + "d".repeat(30) + ".com";
            org.junit.jupiter.api.Assertions.assertTrue(email.length() > 150);
            assertInvalido(usuario("Ana", "Silva", email, "senha123", CURSO_OK, 3), "email", "150");
        }

        @ParameterizedTest(name = "senha valida (len {0})")
        @ValueSource(ints = {4, 5, 8, 31, 32})
        void senhaNosLimitesPassa(int tamanho) {
            assertValido(usuario("Ana", "Silva", "ana@exemplo.com", "x".repeat(tamanho), CURSO_OK, 3));
        }

        @ParameterizedTest(name = "senha fora do limite (len {0})")
        @ValueSource(ints = {1, 2, 3, 33, 64, 1000})
        void senhaForaDoLimiteFalha(int tamanho) {
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "x".repeat(tamanho), CURSO_OK, 3), "senha", "entre 4 e 32");
        }

        @ParameterizedTest(name = "senha vazia/nula/branca [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "    ", "\t\t\t\t"})
        void senhaVaziaNulaOuBranca(String v) {
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", v, CURSO_OK, 3), "senha");
        }

        @ParameterizedTest(name = "senha com conteudo hostil e aceita (e so hasheada) #{index}")
        @ValueSource(strings = {"' OR '1'='1", "<script>1</script>", "😀😀😀😀😀", "Sen\u0000ha", "pässwörd!", "a b c d e"})
        void senhaComConteudoHostilEhApenasTextoOpaco(String v) {
            assertValido(usuario("Ana", "Silva", "ana@exemplo.com", v, CURSO_OK, 3));
        }

        @Test
        void senhaFracaEhAceitaPelaRegraAtual() {
            assertValido(usuario("Ana", "Silva", "ana@exemplo.com", "1234", CURSO_OK, 3));
            assertValido(usuario("Ana", "Silva", "ana@exemplo.com", "aaaa", CURSO_OK, 3));
        }

        @Test
        void cursoNuloFalha() {
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123", null, 3), "curso");
        }

        @Test
        void cursoAninhadoInvalidoApontaCaminhoCompleto() {
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123",
                    new CursoRequestDTO("", "UFMG", 8), 3), "curso.nomeCurso");
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123",
                    new CursoRequestDTO("Curso", null, 8), 3), "curso.instituicao");
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123",
                    new CursoRequestDTO("Curso", "UFMG", 0), 3), "curso.duracao");
        }

        @ParameterizedTest(name = "semestre valido {0}")
        @ValueSource(ints = {1, 2, 10, 19, 20})
        void semestreValido(int s) {
            assertValido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123", CURSO_OK, s));
        }

        @ParameterizedTest(name = "semestre invalido {0}")
        @ValueSource(ints = {0, -1, -100, 21, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
        void semestreInvalido(int s) {
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123", CURSO_OK, s), "semestreAtual");
        }

        @Test
        void semestreNuloFalha() {
            assertInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "senha123", CURSO_OK, null), "semestreAtual", "obrigat");
        }

        @Test
        void corpoTotalmenteVazioReportaTodosOsCamposObrigatorios() {
            var e = erros(usuario(null, null, null, null, null, null));
            org.junit.jupiter.api.Assertions.assertEquals(
                    java.util.Set.of("nome", "sobrenome", "email", "senha", "curso", "semestreAtual"), e.keySet());
        }

        @Test
        void erroEmUmCampoNaoContaminaOsDemais() {
            assertSoEsteCampoInvalido(usuario("João1", "Silva", "ana@exemplo.com", "senha123", CURSO_OK, 3), "nome");
            assertSoEsteCampoInvalido(usuario("Ana", "Silva", "ana@exemplo.com", "abc", CURSO_OK, 3), "senha");
        }
    }

    @Nested
    class AtualizarPerfil {
        @Test
        void validoPassa() {
            assertValido(new UsuarioUpdateDTO("Ana", "Silva", 5));
        }

        @ParameterizedTest(name = "nome invalido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaInvalidos")
        void nomeInvalido(String v) {
            assertInvalido(new UsuarioUpdateDTO(v, "Silva", 5), "nome");
        }

        @ParameterizedTest(name = "sobrenome invalido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaInvalidos")
        void sobrenomeInvalido(String v) {
            assertInvalido(new UsuarioUpdateDTO("Ana", v, 5), "sobrenome");
        }

        @ParameterizedTest(name = "nome/sobrenome vazio ou nulo [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void vazioNulo(String v) {
            assertInvalido(new UsuarioUpdateDTO(v, "Silva", 5), "nome");
            assertInvalido(new UsuarioUpdateDTO("Ana", v, 5), "sobrenome");
        }

        @ParameterizedTest(name = "nome valido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#nomesPessoaValidos")
        void nomeValido(String v) {
            assertValido(new UsuarioUpdateDTO(v, v, 5));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -3, 21, 999})
        void semestreInvalido(int s) {
            assertInvalido(new UsuarioUpdateDTO("Ana", "Silva", s), "semestreAtual");
        }

        @Test
        void semestreNulo() {
            assertInvalido(new UsuarioUpdateDTO("Ana", "Silva", null), "semestreAtual");
        }

        @Test
        void mensagemDoNomeEstaTrocadaComSobrenome() {
            var e = erros(new UsuarioUpdateDTO("João1", "Silva", 5));
            org.junit.jupiter.api.Assertions.assertTrue(e.get("nome").contains("Sobrenome"),
                    "Se este teste falhar, a mensagem foi corrigida: atualize o teste.");
        }
    }

    static Stream<String> textosCursoValidos() {
        return Stream.of("Ciência da Computação", "Engenharia de Software", "Sistemas de Informação (EaD)",
                "Letras - Português/Inglês", "Administração & Negócios", "C1", "Química, Licenciatura", "Ph.D. em Física",
                "D'Água", "A".repeat(150), "2º Ciclo");
    }

    static Stream<String> textosCursoInvalidos() {
        Stream.Builder<String> b = Stream.builder();
        for (String s : new String[]{"A", "A".repeat(151), "-Curso", " Curso", ".Curso", "&Curso", "Curso;", "Curso<>",
                "Curso=1", "Curso@x", "Curso#1", "Curso%", "Curso*", "Curso+", "Curso!", "Curso?", "Curso\\x", "Curso|x",
                "Curso{x}", "Curso[x]", "Curso\u0000", "Curso\n", "😀Curso", "Curso😀"}) b.add(s);
        for (String s : Ataques.SQL_COM_SIMBOLOS) b.add(s);
        for (String s : Ataques.XSS) b.add(s);
        for (String s : Ataques.INJECAO_FORMULA_CSV) b.add(s);
        return b.build();
    }

    @Nested
    class AtualizarCurso {
        @ParameterizedTest(name = "nomeCurso valido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#textosCursoValidos")
        void nomeCursoValido(String v) {
            assertValido(new CursoRequestDTO(v, "UFMG", 8));
        }

        @ParameterizedTest(name = "nomeCurso invalido #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#textosCursoInvalidos")
        void nomeCursoInvalido(String v) {
            assertInvalido(new CursoRequestDTO(v, "UFMG", 8), "nomeCurso");
        }

        @ParameterizedTest(name = "nomeCurso vazio/nulo [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void nomeCursoVazio(String v) {
            assertInvalido(new CursoRequestDTO(v, "UFMG", 8), "nomeCurso");
        }

        @ParameterizedTest(name = "instituicao valida #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#textosCursoValidos")
        void instituicaoValida(String v) {
            assertValido(new CursoRequestDTO("Curso", v, 8));
        }

        @ParameterizedTest(name = "instituicao invalida #{index}")
        @MethodSource("com.example.trinots.validacao.UsuarioAuthDtoValidacaoTest#textosCursoInvalidos")
        void instituicaoInvalida(String v) {
            assertInvalido(new CursoRequestDTO("Curso", v, 8), "instituicao");
        }

        @ParameterizedTest(name = "instituicao vazia/nula [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void instituicaoVazia(String v) {
            assertInvalido(new CursoRequestDTO("Curso", v, 8), "instituicao");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 8, 20})
        void duracaoValida(int d) {
            assertValido(new CursoRequestDTO("Curso", "UFMG", d));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 21, 100, Integer.MAX_VALUE})
        void duracaoInvalida(int d) {
            assertInvalido(new CursoRequestDTO("Curso", "UFMG", d), "duracao");
        }

        @Test
        void duracaoNula() {
            assertInvalido(new CursoRequestDTO("Curso", "UFMG", null), "duracao", "obrigat");
        }
    }

    @Nested
    class TrocarSenha {
        @Test
        void validoPassa() {
            assertValido(new TrocarSenhaDTO("antiga", "novaSenha1", "novaSenha1"));
        }

        @ParameterizedTest(name = "senhaAtual vazia/nula [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void senhaAtualObrigatoria(String v) {
            assertInvalido(new TrocarSenhaDTO(v, "novaSenha1", "novaSenha1"), "senhaAtual");
        }

        @Test
        void senhaAtualNaoTemLimiteDeTamanhoNoDto() {
            assertValido(new TrocarSenhaDTO("x", "novaSenha1", "novaSenha1"));
            assertValido(new TrocarSenhaDTO("x".repeat(500), "novaSenha1", "novaSenha1"));
        }

        @ParameterizedTest(name = "novaSenha fora do limite (len {0})")
        @ValueSource(ints = {0, 1, 3, 33, 200})
        void novaSenhaForaDoLimite(int n) {
            assertInvalido(new TrocarSenhaDTO("antiga", "x".repeat(n), "novaSenha1"), "novaSenha");
        }

        @ParameterizedTest(name = "novaSenha no limite (len {0})")
        @ValueSource(ints = {4, 32})
        void novaSenhaNoLimite(int n) {
            assertValido(new TrocarSenhaDTO("antiga", "x".repeat(n), "x".repeat(n)));
        }

        @ParameterizedTest(name = "novaSenhaConfirmada fora do limite (len {0})")
        @ValueSource(ints = {0, 1, 3, 33})
        void confirmacaoForaDoLimite(int n) {
            assertInvalido(new TrocarSenhaDTO("antiga", "novaSenha1", "x".repeat(n)), "novaSenhaConfirmada");
        }

        @ParameterizedTest(name = "nova/confirmacao nula ou branca [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"      "})
        void novaSenhaNulaOuBranca(String v) {
            assertInvalido(new TrocarSenhaDTO("antiga", v, "novaSenha1"), "novaSenha");
            assertInvalido(new TrocarSenhaDTO("antiga", "novaSenha1", v), "novaSenhaConfirmada");
        }

        @Test
        void divergenciaEntreNovaEConfirmacaoNaoEhValidadaNoDto() {
            assertValido(new TrocarSenhaDTO("antiga", "senhaAAAA", "senhaBBBB"));
        }
    }

    @Nested
    class AlterarMediaAprovacao {
        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.1, 5.0, 6.0, 7.5, 10.0, 50.0, 100.0})
        void valoresAceitos(double v) {
            assertValido(new NovaMediaDTO(v));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.0001, -1.0, -100.0, 100.0001, 101.0, 1e9, Double.MAX_VALUE})
        void valoresRecusados(double v) {
            assertInvalido(new NovaMediaDTO(v), "novaMedia");
        }

        @Test
        void nulo() {
            assertInvalido(new NovaMediaDTO(null), "novaMedia", "obrigat");
        }

        @Test
        void achadoTetoDe100ContradizEscalaDe10() {
            assertValido(new NovaMediaDTO(50.0));
        }
    }

    @Nested
    class ExcluirConta {
        @ParameterizedTest
        @ValueSource(strings = {"senha123", "x", "' OR '1'='1", "😀"})
        void aceitaQualquerSenhaNaoVazia(String v) {
            assertValido(new ExcluirContaDTO(v));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t\n"})
        void senhaObrigatoria(String v) {
            assertInvalido(new ExcluirContaDTO(v), "senha");
        }
    }

    @Nested
    class LoginEReativar {
        @Test
        void validoPassa() {
            assertValido(new LoginRequestDTO("ana@exemplo.com", "senha123"));
        }

        @ParameterizedTest(name = "email invalido [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "semarroba", "@x.com", "a@", "' OR '1'='1", "admin' --", "a b@c.com"})
        void emailInvalido(String v) {
            assertInvalido(new LoginRequestDTO(v, "senha123"), "email");
        }

        @ParameterizedTest(name = "senha invalida [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "abc", "x"})
        void senhaInvalida(String v) {
            assertInvalido(new LoginRequestDTO("ana@exemplo.com", v), "senha");
        }

        @Test
        void senhaMuitoLongaRetorna400AntesDeChegarNoBcrypt() {
            assertInvalido(new LoginRequestDTO("ana@exemplo.com", "x".repeat(33)), "senha");
            assertInvalido(new LoginRequestDTO("ana@exemplo.com", "x".repeat(10_000)), "senha");
        }

        @Test
        void senhaDeLoginComSqlInjectionPassaNaValidacaoMasNuncaVaiParaSql() {
            assertValido(new LoginRequestDTO("ana@exemplo.com", "' OR '1'='1"));
        }

        @Test
        void emailNoLoginTemLimite150() {
            String longo = "a".repeat(64) + "@" + "d".repeat(63) + "." + "d".repeat(63) + ".com";
            org.junit.jupiter.api.Assertions.assertTrue(longo.length() > 150);
            assertInvalido(new LoginRequestDTO(longo, "senha123"), "email");
        }
    }

    @Nested
    class EmailSomente {
        @ParameterizedTest(name = "email valido [{0}]")
        @ValueSource(strings = {"ana@exemplo.com", "ana+x@exemplo.com.br"})
        void valido(String v) {
            assertValido(new SolicitarRecuperacaoDTO(v));
            assertValido(new ReenviarVerificacaoDTO(v));
        }

        @ParameterizedTest(name = "email invalido [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "semarroba", "@x.com", "a@", "'; DROP TABLE usuario;--", "<script>@x"})
        void invalido(String v) {
            assertInvalido(new SolicitarRecuperacaoDTO(v), "email");
            assertInvalido(new ReenviarVerificacaoDTO(v), "email");
        }

        @Test
        void naoHaLimiteDeTamanhoNoEmailDestesDtos() {
            String longo = "a".repeat(64) + "@" + "d".repeat(63) + "." + "d".repeat(63) + ".com";
            assertValido(new SolicitarRecuperacaoDTO(longo));
            assertValido(new ReenviarVerificacaoDTO(longo));
        }
    }

    @Nested
    class CodigoDeSeisDigitos {
        @ParameterizedTest(name = "codigo valido [{0}]")
        @ValueSource(strings = {"000000", "123456", "999999", "012345"})
        void codigoValido(String c) {
            assertValido(new VerificarEmailDTO("ana@exemplo.com", c));
            assertValido(new RedefinirSenhaComTokenDTO("ana@exemplo.com", c, "novaSenha1"));
        }

        @ParameterizedTest(name = "codigo invalido [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "12345", "1234567", "12345a", "abcdef", " 123456", "123456 ", "123 456", "123456\n",
                "12-456", "1.3456", "-12345", "+12345", "１２３４５６", "١٢٣٤٥٦", "' OR '1'='1", "<script>", "1e5555",
                "0x1234", "123456;--"})
        void codigoInvalido(String c) {
            assertInvalido(new VerificarEmailDTO("ana@exemplo.com", c), "codigo");
            assertInvalido(new RedefinirSenhaComTokenDTO("ana@exemplo.com", c, "novaSenha1"), "codigo");
        }

        @ParameterizedTest(name = "email invalido [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "semarroba", "' OR 1=1 --"})
        void emailInvalido(String e) {
            assertInvalido(new VerificarEmailDTO(e, "123456"), "email");
            assertInvalido(new RedefinirSenhaComTokenDTO(e, "123456", "novaSenha1"), "email");
        }

        @ParameterizedTest(name = "novaSenha invalida (len {0})")
        @ValueSource(ints = {0, 1, 3, 33, 500})
        void novaSenhaForaDoLimite(int n) {
            assertInvalido(new RedefinirSenhaComTokenDTO("ana@exemplo.com", "123456", "x".repeat(n)), "novaSenha");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"    "})
        void novaSenhaVaziaOuNula(String v) {
            assertInvalido(new RedefinirSenhaComTokenDTO("ana@exemplo.com", "123456", v), "novaSenha");
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 32})
        void novaSenhaNosLimites(int n) {
            assertValido(new RedefinirSenhaComTokenDTO("ana@exemplo.com", "123456", "x".repeat(n)));
        }
    }
}
