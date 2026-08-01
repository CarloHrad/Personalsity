package com.example.trinots.dto.AvaliacaoDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AvaliacaoConcluirDTO(
        @NotNull(message = "Nota obtida é obrigatória")
        @PositiveOrZero(message = "Nota obtida não pode ser negativa")
        Double notaObtida
) {}