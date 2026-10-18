package com.example.trinots.dto.AvaliacaoDTO;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoRequestDTO(
        @NotBlank(message = "Nome da avaliação é obrigatório")
        @Size(min = 2, max = 150, message = "Nome da avaliação deve ter entre 2 e 150 caracteres")
        String nomeAvaliacao,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "Data da avaliação é obrigatória")
        LocalDate dataAvaliacao,

        @PositiveOrZero(message = "Nota obtida não pode ser negativa")
        @Digits(integer = 4, fraction = 2, message = "Nota obtida deve ter no máximo 4 dígitos inteiros e 2 decimais")
        Double notaObtida,

        @NotNull(message = "Nota máxima é obrigatória")
        @Positive(message = "Nota máxima deve ser maior que zero")
        @DecimalMax(value = "1000.0", message = "Nota máxima deve ser no máximo 1000")
        @Digits(integer = 4, fraction = 2, message = "Nota máxima deve ter no máximo 4 dígitos inteiros e 2 decimais")
        Double notaMaxima,

        @Positive(message = "Peso deve ser maior que zero")
        @DecimalMax(value = "100.0", message = "Peso deve ser no máximo 100")
        @Digits(integer = 3, fraction = 2, message = "Peso deve ter no máximo 3 dígitos inteiros e 2 decimais")
        Double peso,

        @NotNull(message = "Disciplina é obrigatória")
        UUID idDisciplina
) {
        @AssertTrue(message = "Nota obtida não pode ser maior que a nota máxima")
        public boolean isNotaObtidaValida() {
                return notaObtida == null || notaMaxima == null || notaObtida <= notaMaxima;
        }
}