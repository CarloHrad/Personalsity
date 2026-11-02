package com.example.trinots.dto.AvaliacaoDTO;

import jakarta.validation.constraints.PositiveOrZero;

public record AvaliacaoConcluirDTO(
        @PositiveOrZero(message = "Nota obtida não pode ser negativa")
        Double notaObtida
) {}