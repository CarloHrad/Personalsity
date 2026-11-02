package com.example.trinots.service;

import com.example.trinots.domain.enums.TipoMediaEnum;

import java.util.List;

public final class MediaCalculator {

    public static final double ESCALA_MAXIMA = 10.0;
    private static final double TOLERANCIA = 1e-9;

    private MediaCalculator() {
    }

    public record Item(Double notaObtida, Double notaMaxima, Double peso, boolean concluida) {
    }

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
            // Concluída sem nota ("aguardando nota") não entra na média: conta como pendente.
            if (item.concluida() && item.notaObtida() != null) {
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
