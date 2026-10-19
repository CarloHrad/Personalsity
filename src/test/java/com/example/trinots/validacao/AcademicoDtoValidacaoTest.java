package com.example.trinots.validacao;

import com.example.trinots.domain.enums.DiaSemanaEnum;
import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoConcluirDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoRequestDTO;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaRequestDTO;
import com.example.trinots.dto.HorarioDTO.HorarioEmbutidoDTO;
import com.example.trinots.dto.HorarioDTO.HorarioRequestDTO;
import com.example.trinots.dto.TarefaDTO.TarefaRequestDTO;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * POST/PUT /disciplinas, POST/PUT /horarios, POST/PUT /tarefas, POST/PUT /avaliacoes e
 * PATCH /avaliacoes/{id}/concluir.
 */
class AcademicoDtoValidacaoTest extends ValidacaoSuporte {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final HorarioEmbutidoDTO HORARIO_OK =
            new HorarioEmbutidoDTO(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));

    static DisciplinaRequestDTO disc(String nome, Integer periodo, String professor, String sala, Integer andar,
                                     String cor, TipoMediaEnum tipo, List<HorarioEmbutidoDTO> horarios) {
        return new DisciplinaRequestDTO(nome, periodo, professor, sala, andar, cor, tipo, horarios);
    }

    static DisciplinaRequestDTO discOk() {
        return disc("Cálculo I", 1, "Dr. João da Silva", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, List.of(HORARIO_OK));
    }

    static Stream<String> nomesDisciplinaValidos() {
        return Stream.of("Cálculo I", "Algoritmos & Estruturas de Dados", "Engenharia de Software (EaD)", "Física 2",
                "Inglês - Nível 3", "Laboratório/Prática", "D'Água", "Cálculo, Lógica e Prova", "AB", "1º Ciclo", "A".repeat(150),
                "Cal' OR 'x' OR '");
    }

    static Stream<String> nomesDisciplinaInvalidos() {
        Stream.Builder<String> b = Stream.builder();
        for (String s : new String[]{"A", "A".repeat(151), "-Cálculo", " Cálculo", "'Cálculo", ".Cálculo", "&Cálculo",
                "C++", "C#", "Cálculo;", "Cálculo=1", "Cálculo<", "Cálculo>", "Cálculo@", "Cálculo!", "Cálculo?",
                "Cálculo%", "Cálculo*", "Cálculo\\", "Cálculo|", "Cálculo[1]", "Cálculo{1}", "Cálculo\n", "Cálculo\u0000",
                "Cálculo\t", "😀", "Cálculo 😀"}) b.add(s);
        for (String s : Ataques.SQL_COM_SIMBOLOS) b.add(s);
        for (String s : Ataques.XSS) b.add(s);
        for (String s : Ataques.INJECAO_FORMULA_CSV) b.add(s);
        return b.build();
    }

    static Stream<String> professoresValidos() {
        return Stream.of("João", "Dr. João da Silva", "Prof.ª Maria", "Maria (Coord.)", "O’Brien", "D'Ávila", "José-Luís",
                "Çelik", "Ünal Ölçer", "Ng", "Silva, João", "Łukasz Żółć", "Nguyễn Văn", "李雷", "A".repeat(100));
    }

    static Stream<String> professoresInvalidos() {
        Stream.Builder<String> b = Stream.builder();
        for (String s : new String[]{"3João", "João123", "João1", "João@", "João#", "João$", "João%", "João&Maria", "João*",
                "João+", "João=", "João/Silva", "João\\Silva", "João;", "João:", "João?", "João!", "João[1]", "João{1}",
                "João|", "João_Silva", "João\n", "João\t", "João\u0000", "😀", "João😀", "'João", ".João", "-João", " João",
                "(João)", "A".repeat(101)}) b.add(s);
        for (String s : Ataques.SQL_COM_SIMBOLOS) b.add(s);
        for (String s : Ataques.XSS) b.add(s);
        for (String s : Ataques.INJECAO_FORMULA_CSV) b.add(s);
        return b.build();
    }

    static Stream<String> salasValidas() {
        return Stream.of("A101", "101", "Lab. 3", "B-12/2", "Sala 12", "Bloco A", "Auditório", "A".repeat(20), "1.2.3");
    }

    static Stream<String> salasInvalidas() {
        Stream.Builder<String> b = Stream.builder();
        for (String s : new String[]{"A".repeat(21), "-A1", " A1", ".A1", "/A1", "A1;", "A1'", "A#1", "A1<", "A1>", "A1=1",
                "A1(2)", "A1&2", "A1_2", "A1,2", "A1\n", "A1\u0000", "😀"}) b.add(s);
        for (String s : Ataques.SQL_COM_SIMBOLOS) b.add(s);
        for (String s : Ataques.XSS) b.add(s);
        return b.build();
    }

    @Nested
    class Disciplina {

        @Test
        void corpoValidoPassa() {
            assertValido(discOk());
        }

        @Test
        void corpoSemHorariosNemAndarPassa() {
            assertValido(disc("Cálculo I", 1, "Dr. João", "A101", null, "#FFF", TipoMediaEnum.PONDERADA, null));
            assertValido(disc("Cálculo I", 1, "Dr. João", "A101", null, "#FFF", TipoMediaEnum.PONDERADA, List.of()));
        }

        @ParameterizedTest(name = "nomeDisciplina valido #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#nomesDisciplinaValidos")
        void nomeValido(String v) {
            assertValido(disc(v, 1, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null));
        }

        @ParameterizedTest(name = "nomeDisciplina invalido #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#nomesDisciplinaInvalidos")
        void nomeInvalido(String v) {
            assertInvalido(disc(v, 1, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "nomeDisciplina");
        }

        @ParameterizedTest(name = "nomeDisciplina vazio/nulo [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        void nomeVazioOuNulo(String v) {
            assertInvalido(disc(v, 1, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "nomeDisciplina");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 10, 20})
        void periodoValido(int p) {
            assertValido(disc("Cálculo I", p, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, 21, 99, Integer.MAX_VALUE, Integer.MIN_VALUE})
        void periodoInvalido(int p) {
            assertInvalido(disc("Cálculo I", p, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "periodo");
        }

        @Test
        void periodoNulo() {
            assertInvalido(disc("Cálculo I", null, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "periodo", "obrigat");
        }

        @ParameterizedTest(name = "professor valido #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#professoresValidos")
        void professorValido(String v) {
            assertValido(disc("Cálculo I", 1, v, "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null));
        }

        @ParameterizedTest(name = "professor invalido #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#professoresInvalidos")
        void professorInvalido(String v) {
            assertInvalido(disc("Cálculo I", 1, v, "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "professor");
        }

        @ParameterizedTest(name = "professor vazio/nulo [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void professorVazioOuNulo(String v) {
            assertInvalido(disc("Cálculo I", 1, v, "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "professor", "obrigat");
        }

        @ParameterizedTest(name = "sala valida #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#salasValidas")
        void salaValida(String v) {
            assertValido(disc("Cálculo I", 1, "Dr. João", v, 1, "#FF5733", TipoMediaEnum.SIMPLES, null));
        }

        @ParameterizedTest(name = "sala invalida #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#salasInvalidas")
        void salaInvalida(String v) {
            assertInvalido(disc("Cálculo I", 1, "Dr. João", v, 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "sala");
        }

        @ParameterizedTest(name = "sala vazia/nula [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t"})
        void salaVaziaOuNula(String v) {
            assertInvalido(disc("Cálculo I", 1, "Dr. João", v, 1, "#FF5733", TipoMediaEnum.SIMPLES, null), "sala", "obrigat");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 10, 200})
        void andarValido(int a) {
            assertValido(disc("Cálculo I", 1, "Dr. João", "A101", a, "#FF5733", TipoMediaEnum.SIMPLES, null));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -100, 201, 1000, Integer.MAX_VALUE, Integer.MIN_VALUE})
        void andarInvalido(int a) {
            assertInvalido(disc("Cálculo I", 1, "Dr. João", "A101", a, "#FF5733", TipoMediaEnum.SIMPLES, null), "andar");
        }

        @ParameterizedTest(name = "cor valida [{0}]")
        @ValueSource(strings = {"#FFF", "#fff", "#FF5733", "#ff5733", "#000000", "#abc123", "#A1B2C3"})
        void corValida(String v) {
            assertValido(disc("Cálculo I", 1, "Dr. João", "A101", 1, v, TipoMediaEnum.SIMPLES, null));
        }

        @ParameterizedTest(name = "cor invalida [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "FF5733", "#GGGGGG", "#FFFF", "#FF", "#", "#FF57331", "# FF5733", "#FF5733 ", " #FF5733",
                "red", "rgb(255,0,0)", "#FF5733;", "#FF5733'--", "'; DROP TABLE disciplina;--", "<script>", "#ＦＦＦ",
                "#FF5733\n", "javascript:alert(1)", "url(x)"})
        void corInvalida(String v) {
            assertInvalido(disc("Cálculo I", 1, "Dr. João", "A101", 1, v, TipoMediaEnum.SIMPLES, null), "cor");
        }

        @Test
        void tipoMediaNulo() {
            assertInvalido(disc("Cálculo I", 1, "Dr. João", "A101", 1, "#FF5733", null, null), "tipoMedia", "obrigat");
        }

        @ParameterizedTest
        @ValueSource(strings = {"SIMPLES", "PONDERADA"})
        void tipoMediaValido(String nome) {
            assertValido(disc("Cálculo I", 1, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.valueOf(nome), null));
        }

        @Test
        void horarioEmbutidoInvalidoApontaIndiceECampo() {
            var h = new HorarioEmbutidoDTO(null, LocalTime.of(8, 0), LocalTime.of(10, 0));
            assertInvalido(disc("Cálculo I", 1, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, List.of(HORARIO_OK, h)),
                    "horarios[1].diaSemana");
        }

        @Test
        void horarioEmbutidoComFimAntesDoInicio() {
            var h = new HorarioEmbutidoDTO(DiaSemanaEnum.TERCA, LocalTime.of(10, 0), LocalTime.of(9, 0));
            assertInvalido(disc("Cálculo I", 1, "Dr. João", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, List.of(h)),
                    "horarios[0].horarioValido");
        }

        @Test
        void todosOsCamposVaziosReportamObrigatorios() {
            var e = erros(disc(null, null, null, null, null, null, null, null));
            assertEquals(java.util.Set.of("nomeDisciplina", "periodo", "professor", "sala", "cor", "tipoMedia"), e.keySet());
        }

        @Test
        void nomeDeProfessorComAspasPassaMasEhInerteNoBanco() {
            assertValido(disc("Cálculo I", 1, "Maria' OR 'a' OR '", "A101", 1, "#FF5733", TipoMediaEnum.SIMPLES, null));
        }
    }

    static HorarioRequestDTO hor(DiaSemanaEnum d, LocalTime i, LocalTime f, UUID id) {
        return new HorarioRequestDTO(d, i, f, id);
    }

    static HorarioEmbutidoDTO emb(DiaSemanaEnum d, LocalTime i, LocalTime f) {
        return new HorarioEmbutidoDTO(d, i, f);
    }

    @Nested
    class Horario {

        @Test
        void validoPassa() {
            assertValido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0), ID));
            assertValido(emb(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO"})
        void todosOsDiasDaSemana(String d) {
            assertValido(hor(DiaSemanaEnum.valueOf(d), LocalTime.of(8, 0), LocalTime.of(9, 0), ID));
        }

        @Test
        void camposNulos() {
            assertInvalido(hor(null, LocalTime.of(8, 0), LocalTime.of(9, 0), ID), "diaSemana", "obrigat");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, null, LocalTime.of(9, 0), ID), "horaInicio", "obrigat");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), null, ID), "horaFim", "obrigat");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(9, 0), null), "idDisciplina", "obrigat");
            assertEquals(java.util.Set.of("diaSemana", "horaInicio", "horaFim", "idDisciplina"),
                    erros(hor(null, null, null, null)).keySet());
        }

        @Test
        void horaFimIgualOuAnteriorAoInicio() {
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(9, 0), LocalTime.of(9, 0), ID), "horarioValido");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(10, 0), LocalTime.of(9, 0), ID), "horarioValido");
            assertInvalido(emb(DiaSemanaEnum.SEGUNDA, LocalTime.of(10, 0), LocalTime.of(9, 0)), "horarioValido");
        }

        @Test
        void limitesDoIntervaloPermitido() {
            assertValido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(6, 0), LocalTime.of(7, 0), ID));
            assertValido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(22, 0), LocalTime.of(23, 59), ID));
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(5, 59), LocalTime.of(7, 0), ID), "dentroDoIntervaloPermitido");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(0, 0), LocalTime.of(1, 0), ID), "dentroDoIntervaloPermitido");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(22, 0), LocalTime.of(23, 59, 1), ID), "dentroDoIntervaloPermitido");
            assertInvalido(emb(DiaSemanaEnum.SEGUNDA, LocalTime.of(5, 0), LocalTime.of(7, 0)), "dentroDoIntervaloPermitido");
        }

        @Test
        void duracaoMinimaDe15Minutos() {
            assertValido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(8, 15), ID));
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(8, 14), ID), "duracaoMinimaValida");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(8, 1), ID), "duracaoMinimaValida");
            assertInvalido(emb(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(8, 14)), "duracaoMinimaValida");
        }

        @Test
        void duracaoMaximaDe8Horas() {
            assertValido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(16, 0), ID));
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(16, 1), ID), "duracaoMaximaValida");
            assertInvalido(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(6, 0), LocalTime.of(23, 59), ID), "duracaoMaximaValida");
            assertInvalido(emb(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(16, 1)), "duracaoMaximaValida");
        }

        @Test
        void fimAntesDoInicioNaoGeraErrosDeDuracaoEmCascata() {
            var e = erros(hor(DiaSemanaEnum.SEGUNDA, LocalTime.of(10, 0), LocalTime.of(9, 0), ID));
            assertEquals(java.util.Set.of("horarioValido"), e.keySet(), e.toString());
        }
    }

    static TarefaRequestDTO tar(String nome, String desc, LocalDate data, LocalTime hora, UUID id) {
        return new TarefaRequestDTO(nome, desc, data, hora, id);
    }

    static Stream<String> nomesTarefaValidos() {
        return Stream.of("Lista 1", "Lista 1: Exercícios (cap. 2)", "Entrega_final+extra", "Trabalho A&B", "Prova/Revisão",
                "Resumo - Cap 3", "AB", "A".repeat(150), "Relatório, versão 2", "D'Água");
    }

    static Stream<String> nomesTarefaInvalidos() {
        Stream.Builder<String> b = Stream.builder();
        for (String s : new String[]{"A", "A".repeat(151), "-Lista", "_Lista", " Lista", "+Lista", ":Lista", "Lista;", "Lista=1",
                "Lista<", "Lista>", "Lista@", "Lista#", "Lista%", "Lista!", "Lista?", "Lista*", "Lista\\", "Lista|", "Lista[1]",
                "Lista{1}", "Lista\n", "Lista\u0000", "Lista\t", "😀", "Lista 😀"}) b.add(s);
        for (String s : Ataques.SQL_COM_SIMBOLOS) b.add(s);
        for (String s : Ataques.XSS) b.add(s);
        for (String s : Ataques.INJECAO_FORMULA_CSV) b.add(s);
        return b.build();
    }

    @Nested
    class Tarefa {
        private final LocalDate DATA = LocalDate.of(2030, 5, 20);

        @Test
        void textosComCaracteresPermitidosPassamMasSaoInertes() {
            assertValido(tar(Ataques.XSS_URI, null, DATA, null, ID));
            for (String s : Ataques.SQL_SO_LETRAS) {
                if (!s.startsWith("'")) assertValido(tar(s, null, DATA, null, ID));
                else assertInvalido(tar(s, null, DATA, null, ID), "nomeTarefa");
            }
        }

        @Test
        void validaPassa() {
            assertValido(tar("Lista 1", "Resolver os exercícios", DATA, LocalTime.of(23, 59), ID));
            assertValido(tar("Lista 1", null, DATA, null, ID));
        }

        @ParameterizedTest(name = "nomeTarefa valido #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#nomesTarefaValidos")
        void nomeValido(String v) {
            assertValido(tar(v, null, DATA, null, ID));
        }

        @ParameterizedTest(name = "nomeTarefa invalido #{index}")
        @MethodSource("com.example.trinots.validacao.AcademicoDtoValidacaoTest#nomesTarefaInvalidos")
        void nomeInvalido(String v) {
            assertInvalido(tar(v, null, DATA, null, ID), "nomeTarefa");
        }

        @ParameterizedTest(name = "nomeTarefa vazio/nulo [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t"})
        void nomeVazioOuNulo(String v) {
            assertInvalido(tar(v, null, DATA, null, ID), "nomeTarefa");
        }

        @ParameterizedTest(name = "descricao valida #{index}")
        @ValueSource(strings = {"", "Resolver os exercícios 1 a 10.", "Linha 1\nLinha 2", "Com tab\taqui", "Preço: R$ 10,50 (50%)",
                "Link: https://exemplo.com/a?b=c&d=e", "Emoji 😀 ok", "Aspas 'simples' e \"duplas\"", "Acentuação: ção, ã, é, ü",
                "'; DROP TABLE tarefa; --", "' OR '1'='1", "<script>alert(1)</script>", "=cmd|' /C calc'!A0"})
        void descricaoValida(String v) {
            assertValido(tar("Lista 1", v, DATA, null, ID));
        }

        @Test
        void descricaoNula() {
            assertValido(tar("Lista 1", null, DATA, null, ID));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 250, 499, 500})
        void descricaoNoLimite(int n) {
            assertValido(tar("Lista 1", "a".repeat(n), DATA, null, ID));
        }

        @ParameterizedTest
        @ValueSource(ints = {501, 1000, 100_000})
        void descricaoAcimaDoLimite(int n) {
            assertInvalido(tar("Lista 1", "a".repeat(n), DATA, null, ID), "descricao", "500");
        }

        @ParameterizedTest(name = "descricao com caractere de controle #{index}")
        @ValueSource(strings = {"texto\u0000nulo", "texto\u0007bell", "texto\u001Bescape", "texto\u007Fdel"})
        void descricaoComControleInvalida(String v) {
            assertInvalido(tar("Lista 1", v, DATA, null, ID), "descricao");
        }

        @Test
        void achadoDescricaoAceitaHtmlEFormulaCsvComoTexto() {
            assertValido(tar("Lista 1", "<img src=x onerror=alert(1)>", DATA, null, ID));
        }

        @Test
        void dataEHoraEDisciplina() {
            assertInvalido(tar("Lista 1", null, null, null, ID), "dataEntrega", "obrigat");
            assertInvalido(tar("Lista 1", null, DATA, null, null), "idDisciplina", "obrigat");
            assertValido(tar("Lista 1", null, LocalDate.of(1900, 1, 1), LocalTime.MIDNIGHT, ID));
        }

        @Test
        void todosOsObrigatoriosVaziosReportamTres() {
            assertEquals(java.util.Set.of("nomeTarefa", "dataEntrega", "idDisciplina"),
                    erros(tar(null, null, null, null, null)).keySet());
        }
    }

    static AvaliacaoRequestDTO av(String nome, String desc, LocalDate data, Double obtida, Double maxima, Double peso, UUID id) {
        return new AvaliacaoRequestDTO(nome, desc, data, obtida, maxima, peso, id);
    }

    @Nested
    class Avaliacao {
        private final LocalDate DATA = LocalDate.of(2030, 6, 10);

        @Test
        void validaPassa() {
            assertValido(av("Prova 1", "Capítulos 1 a 3", DATA, null, 10.0, 3.0, ID));
            assertValido(av("Prova 1", null, DATA, 7.5, 10.0, null, ID));
            assertValido(av("AB", null, DATA, 0.0, 0.01, 0.01, ID));
        }

        @ParameterizedTest(name = "nomeAvaliacao vazio/nulo [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "A"})
        void nomeInvalido(String v) {
            assertInvalido(av(v, null, DATA, null, 10.0, null, ID), "nomeAvaliacao");
        }

        @ParameterizedTest
        @ValueSource(ints = {2, 100, 150})
        void nomeNoLimite(int n) {
            assertValido(av("a".repeat(n), null, DATA, null, 10.0, null, ID));
        }

        @ParameterizedTest
        @ValueSource(ints = {151, 1000})
        void nomeAcimaDoLimite(int n) {
            assertInvalido(av("a".repeat(n), null, DATA, null, 10.0, null, ID), "nomeAvaliacao");
        }

        @Test
        void achadoNomeDaAvaliacaoNaoTemPattern() {
            for (String s : Ataques.SQL_COM_SIMBOLOS) assertValido(av(s, null, DATA, null, 10.0, null, ID));
            for (String s : Ataques.XSS) assertValido(av(s, null, DATA, null, 10.0, null, ID));
            for (String s : Ataques.INJECAO_FORMULA_CSV) assertValido(av(s, null, DATA, null, 10.0, null, ID));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 500})
        void descricaoNoLimite(int n) {
            assertValido(av("Prova 1", "a".repeat(n), DATA, null, 10.0, null, ID));
        }

        @Test
        void descricaoAcimaDoLimite() {
            assertInvalido(av("Prova 1", "a".repeat(501), DATA, null, 10.0, null, ID), "descricao");
        }

        @Test
        void dataObrigatoria() {
            assertInvalido(av("Prova 1", null, null, null, 10.0, null, ID), "dataAvaliacao", "obrigat");
        }

        @Test
        void disciplinaObrigatoria() {
            assertInvalido(av("Prova 1", null, DATA, null, 10.0, null, null), "idDisciplina", "obrigat");
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.01, 5.5, 7.25, 10.0})
        void notaObtidaValida(double n) {
            assertValido(av("Prova 1", null, DATA, n, 10.0, null, ID));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.01, -1.0, -100.0})
        void notaObtidaNegativa(double n) {
            assertInvalido(av("Prova 1", null, DATA, n, 10.0, null, ID), "notaObtida");
        }

        @ParameterizedTest
        @ValueSource(doubles = {7.555, 7.001, 0.001, 3.14159})
        void notaObtidaComMaisDeDuasCasas(double n) {
            assertInvalido(av("Prova 1", null, DATA, n, 1000.0, null, ID), "notaObtida", "decimais");
        }

        @Test
        void notaObtidaMaiorQueMaximaApontaCampoCalculado() {
            assertInvalido(av("Prova 1", null, DATA, 10.01, 10.0, null, ID), "notaObtidaValida");
            assertInvalido(av("Prova 1", null, DATA, 11.0, 10.0, null, ID), "notaObtidaValida");
            assertValido(av("Prova 1", null, DATA, 10.0, 10.0, null, ID));
        }

        @Test
        void notaObtidaMuitoGrandeFalhaPorDigitos() {
            assertInvalido(av("Prova 1", null, DATA, 12345.0, 1000.0, null, ID), "notaObtida");
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.01, 1.0, 5.0, 10.0, 100.0, 999.99, 1000.0})
        void notaMaximaValida(double n) {
            assertValido(av("Prova 1", null, DATA, null, n, null, ID));
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, -0.01, -1.0, -10.0, 1000.01, 1001.0, 99999.0})
        void notaMaximaInvalida(double n) {
            assertInvalido(av("Prova 1", null, DATA, null, n, null, ID), "notaMaxima");
        }

        @ParameterizedTest
        @ValueSource(doubles = {10.001, 7.555})
        void notaMaximaComMaisDeDuasCasas(double n) {
            assertInvalido(av("Prova 1", null, DATA, null, n, null, ID), "notaMaxima", "decimais");
        }

        @Test
        void notaMaximaNula() {
            assertInvalido(av("Prova 1", null, DATA, null, null, null, ID), "notaMaxima", "obrigat");
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.01, 0.5, 1.0, 2.5, 50.0, 99.99, 100.0})
        void pesoValido(double p) {
            assertValido(av("Prova 1", null, DATA, null, 10.0, p, ID));
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, -0.01, -1.0, 100.01, 101.0, 1000.0})
        void pesoInvalido(double p) {
            assertInvalido(av("Prova 1", null, DATA, null, 10.0, p, ID), "peso");
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.001, 1.234, 33.333})
        void pesoComMaisDeDuasCasas(double p) {
            assertInvalido(av("Prova 1", null, DATA, null, 10.0, p, ID), "peso", "decimais");
        }

        @Test
        void pesoNuloEhPermitidoNoDto() {
            assertValido(av("Prova 1", null, DATA, null, 10.0, null, ID));
        }

        @Test
        void todosOsObrigatoriosVazios() {
            assertEquals(java.util.Set.of("nomeAvaliacao", "dataAvaliacao", "notaMaxima", "idDisciplina"),
                    erros(av(null, null, null, null, null, null, null)).keySet());
        }
    }

    @Nested
    class AvaliacaoConcluir {
        @ParameterizedTest
        @ValueSource(doubles = {0.0, 0.5, 7.0, 10.0, 1000.0, 99999.0})
        void notaValida(double n) {
            assertValido(new AvaliacaoConcluirDTO(n));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.0001, -1.0, -1000.0})
        void notaNegativa(double n) {
            assertInvalido(new AvaliacaoConcluirDTO(n), "notaObtida");
        }

        @Test
        void notaNula() {
            assertInvalido(new AvaliacaoConcluirDTO(null), "notaObtida", "obrigat");
        }

        @Test
        void naoValidaLimiteSuperiorNemCasasDecimaisNoDto() {
            assertTrue(erros(new AvaliacaoConcluirDTO(7.555)).isEmpty());
            assertTrue(erros(new AvaliacaoConcluirDTO(99999.0)).isEmpty());
        }
    }
}
