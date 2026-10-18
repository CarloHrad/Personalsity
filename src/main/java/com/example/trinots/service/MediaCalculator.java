package com.example.trinots.service;

import com.example.trinots.domain.enums.TipoMediaEnum;

import java.util.List;

/**
 * Regras de cálculo de média, sem dependência de Spring/JPA para facilitar o teste.
 *
 * <ul>
 *   <li>Toda nota é normalizada para a escala 0–10: {@code nota / notaMaxima * 10}
 *       (ex.: 35 de 50 vira 7.0).</li>
 *   <li>A média atual considera apenas avaliações concluídas; avaliações pendentes ficam fora.</li>
 *   <li>O zero para avaliações pendentes só aparece na projeção pessimista.</li>
 *   <li>Risco de reprovação: a nota necessária nas avaliações restantes (escala 0–10)
 *       é maior que a nota máxima possível (10).</li>
 * </ul>
 */
public final class MediaCalculator {

    public static final double ESCALA_MAXIMA = 10.0;
    private static final double TOLERANCIA = 1e-9;

    private MediaCalculator() {
    }

    /** Dados mínimos de uma avaliação para o cálculo. */
    public record Item(Double notaObtida, Double notaMaxima, Double peso, boolean concluida) {
    }

    /**
     * @param mediaAtual              média das concluídas (0–10); null se não há concluídas ou tipo indefinido
     * @param projecaoPessimista      média considerando as pendentes como 0; null se não há avaliações ou tipo indefinido
     * @param notaNecessariaRestantes média (0–10) que as pendentes precisam atingir para alcançar a média de
     *                                aprovação; null se não há pendentes
     * @param emRiscoReprovacao       true se a nota necessária for maior que 10 (aprovação inalcançável)
     * @param todasConcluidas         true se há avaliações e nenhuma está pendente
     */
    public record Resultado(Double mediaAtual,
                            Double projecaoPessimista,
                            Double notaNecessariaRestantes,
                            boolean emRiscoReprovacao,
                            boolean todasConcluidas) {
    }

    /** Converte a nota para a escala 0–10 (multiplica antes de dividir para evitar ruído de ponto flutuante). */
    public static double normalizar(double notaObtida, double notaMaxima) {
        if (notaMaxima <= 0) {
            return 0.0;
        }
        return notaObtida * ESCALA_MAXIMA / notaMaxima;
    }

    public static Resultado calcular(TipoMediaEnum tipo, List<Item> itens, double mediaAprovacao) {
        if (tipo == null || itens == null || itens.isEmpty()) {
            return new Resultado(null, null, null, false, false);
        }

        double somaPesosTotal = 0.0;
        double somaPesosConcluidas = 0.0;
        double somaPesosPendentes = 0.0;
        double somaPonderadaConcluidas = 0.0;
        int pendentes = 0;
        int concluidas = 0;

        for (Item item : itens) {
            double peso = peso(tipo, item);
            somaPesosTotal += peso;
            if (item.concluida()) {
                concluidas++;
                somaPesosConcluidas += peso;
                somaPonderadaConcluidas += notaNormalizada(item) * peso;
            } else {
                pendentes++;
                somaPesosPendentes += peso;
            }
        }

        Double mediaAtual = null;
        if (concluidas > 0) {
            mediaAtual = somaPesosConcluidas == 0 ? 0.0 : somaPonderadaConcluidas / somaPesosConcluidas;
        }

        double projecao = somaPesosTotal == 0 ? 0.0 : somaPonderadaConcluidas / somaPesosTotal;

        Double notaNecessaria = null;
        boolean emRisco = false;
        if (pendentes > 0 && somaPesosPendentes > 0) {
            double necessaria = (mediaAprovacao * somaPesosTotal - somaPonderadaConcluidas) / somaPesosPendentes;
            notaNecessaria = Math.max(0.0, necessaria);
            emRisco = notaNecessaria > ESCALA_MAXIMA + TOLERANCIA;
        }

        return new Resultado(mediaAtual, projecao, notaNecessaria, emRisco, pendentes == 0);
    }

    /** Nota de uma avaliação na escala 0–10 (0 se ainda não houver nota). */
    public static double notaNormalizada(Item item) {
        if (item.notaObtida() == null || item.notaMaxima() == null) {
            return 0.0;
        }
        return normalizar(item.notaObtida(), item.notaMaxima());
    }

    private static double peso(TipoMediaEnum tipo, Item item) {
        if (tipo == TipoMediaEnum.SIMPLES) {
            return 1.0;
        }
        return item.peso() == null ? 0.0 : item.peso();
    }
}
