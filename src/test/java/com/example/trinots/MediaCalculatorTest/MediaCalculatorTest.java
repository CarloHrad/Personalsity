package com.example.trinots.MediaCalculatorTest;

import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.service.MediaCalculator;
import com.example.trinots.service.MediaCalculator.Item;
import com.example.trinots.service.MediaCalculator.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("MediaCalculator")
class MediaCalculatorTest {

    private static final double MEDIA_APROVACAO = 6.0;

    private static Item concluida(double nota, double max, double peso) {
        return new Item(nota, max, peso, true);
    }

    private static Item pendente(double max, double peso) {
        return new Item(null, max, peso, false);
    }

    @Nested
    @DisplayName("normalização para a escala 0–10")
    class Normalizacao {

        @Test
        @DisplayName("35 de 50 vira exatamente 7.0")
        void trintaECincoDeCinquentaViraSete() {
            assertThat(MediaCalculator.normalizar(35, 50)).isEqualTo(7.0);
        }

        @Test
        @DisplayName("nota máxima inválida (<= 0) não causa divisão por zero")
        void notaMaximaInvalida() {
            assertThat(MediaCalculator.normalizar(5, 0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("avaliações em escalas diferentes entram na média já normalizadas")
        void escalasMistas() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(8, 10, 1), concluida(40, 50, 1)), MEDIA_APROVACAO);

            assertThat(r.mediaAtual()).isEqualTo(8.0);
        }
    }

    @Nested
    @DisplayName("média atual")
    class MediaAtual {

        @Test
        @DisplayName("SIMPLES: média aritmética")
        void simples() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(8, 10, 1), concluida(6, 10, 1), concluida(10, 10, 1)), MEDIA_APROVACAO);

            assertThat(r.mediaAtual()).isEqualTo(8.0);
            assertThat(r.todasConcluidas()).isTrue();
            assertThat(r.notaNecessariaRestantes()).isNull();
            assertThat(r.emRiscoReprovacao()).isFalse();
        }

        @Test
        @DisplayName("PONDERADA: pesos livres, sem exigir soma 100")
        void ponderada() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.PONDERADA,
                    List.of(concluida(8, 10, 3), concluida(4, 10, 1)), MEDIA_APROVACAO);

            assertThat(r.mediaAtual()).isEqualTo(7.0);
        }

        @Test
        @DisplayName("avaliação pendente fica fora da média atual")
        void pendenteForaDaMedia() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(10, 10, 1), pendente(10, 1)), MEDIA_APROVACAO);

            assertThat(r.mediaAtual()).isEqualTo(10.0);
            assertThat(r.todasConcluidas()).isFalse();
        }

        @Test
        @DisplayName("soma de pesos zero não lança exceção")
        void somaDePesosZero() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.PONDERADA,
                    List.of(concluida(8, 10, 0)), MEDIA_APROVACAO);

            assertThat(r.mediaAtual()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("sem tipo de média, sem avaliações ou só com pendentes: média atual é nula")
        void semMediaCalculavel() {
            assertThat(MediaCalculator.calcular(null, List.of(concluida(8, 10, 1)), MEDIA_APROVACAO).mediaAtual()).isNull();
            assertThat(MediaCalculator.calcular(TipoMediaEnum.SIMPLES, List.of(), MEDIA_APROVACAO).mediaAtual()).isNull();
            assertThat(MediaCalculator.calcular(TipoMediaEnum.SIMPLES, List.of(pendente(10, 1)), MEDIA_APROVACAO).mediaAtual()).isNull();
        }

        @Test
        @DisplayName("sem avaliações cadastradas não conta como 'todas concluídas'")
        void semAvaliacoesNaoEstaConcluida() {
            assertThat(MediaCalculator.calcular(TipoMediaEnum.SIMPLES, List.of(), MEDIA_APROVACAO).todasConcluidas()).isFalse();
        }
    }

    @Nested
    @DisplayName("projeção pessimista")
    class Projecao {

        @Test
        @DisplayName("pendentes entram como zero")
        void pendenteEntraComoZero() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(10, 10, 1), pendente(10, 1)), MEDIA_APROVACAO);

            assertThat(r.projecaoPessimista()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("PONDERADA: o peso da pendente conta no denominador")
        void ponderada() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.PONDERADA,
                    List.of(concluida(4, 10, 3), pendente(10, 7)), MEDIA_APROVACAO);

            assertThat(r.projecaoPessimista()).isCloseTo(1.2, within(1e-9));
        }

        @Test
        @DisplayName("só pendentes: projeção é 0")
        void soPendentes() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES, List.of(pendente(10, 1)), MEDIA_APROVACAO);

            assertThat(r.projecaoPessimista()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("nota necessária e risco de reprovação")
    class Risco {

        @Test
        @DisplayName("SIMPLES: nota necessária nas restantes para atingir a média de aprovação")
        void simples() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(10, 10, 1), pendente(10, 1)), MEDIA_APROVACAO);

            assertThat(r.notaNecessariaRestantes()).isEqualTo(2.0);
            assertThat(r.emRiscoReprovacao()).isFalse();
        }

        @Test
        @DisplayName("em risco quando a nota necessária é maior que 10")
        void emRisco() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(1, 10, 1), concluida(0, 10, 1), pendente(10, 1)), MEDIA_APROVACAO);

            assertThat(r.notaNecessariaRestantes()).isEqualTo(17.0);
            assertThat(r.emRiscoReprovacao()).isTrue();
        }

        @Test
        @DisplayName("nota necessária exatamente 10 ainda não é risco (limite inclusivo)")
        void limiteExato() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.PONDERADA,
                    List.of(concluida(2, 10, 5), pendente(10, 5)), MEDIA_APROVACAO);

            assertThat(r.notaNecessariaRestantes()).isEqualTo(10.0);
            assertThat(r.emRiscoReprovacao()).isFalse();
        }

        @Test
        @DisplayName("PONDERADA: um pouco acima de 10 já é risco")
        void ponderadaAcimaDoLimite() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.PONDERADA,
                    List.of(concluida(1, 10, 5), pendente(10, 5)), MEDIA_APROVACAO);

            assertThat(r.emRiscoReprovacao()).isTrue();
        }

        @Test
        @DisplayName("quando as notas já garantem a aprovação, a nota necessária é 0 (nunca negativa)")
        void jaGarantido() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(10, 10, 1), concluida(10, 10, 1), pendente(10, 1)), MEDIA_APROVACAO);

            assertThat(r.notaNecessariaRestantes()).isEqualTo(0.0);
            assertThat(r.emRiscoReprovacao()).isFalse();
        }

        @Test
        @DisplayName("sem pendentes não há nota necessária nem risco")
        void semPendentes() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES, List.of(concluida(0, 10, 1)), MEDIA_APROVACAO);

            assertThat(r.notaNecessariaRestantes()).isNull();
            assertThat(r.emRiscoReprovacao()).isFalse();
        }

        @Test
        @DisplayName("média de aprovação acima de 10 torna a aprovação inalcançável")
        void mediaDeAprovacaoAcimaDeDez() {
            Resultado r = MediaCalculator.calcular(TipoMediaEnum.SIMPLES,
                    List.of(concluida(8, 10, 1), pendente(10, 1)), 12.0);

            assertThat(r.emRiscoReprovacao()).isTrue();
        }
    }
}
