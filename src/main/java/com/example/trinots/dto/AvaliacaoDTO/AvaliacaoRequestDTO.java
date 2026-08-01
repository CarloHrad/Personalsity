package com.example.trinots.dto.AvaliacaoDTO;

import com.example.trinots.domain.enums.TipoAvaliacaoEnum;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoRequestDTO(
        @NotBlank(message = "Nome da avaliação é obrigatório")
        @Size(min = 2, max = 150, message = "Nome da avaliação deve ter entre 2 e 150 caracteres")
        String nomeAvaliacao,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "Tipo de avaliação é obrigatório")
        TipoAvaliacaoEnum tipoAvaliacao,

        @NotNull(message = "Data da avaliação é obrigatória")
        LocalDate dataAvaliacao,

        @NotNull(message = "Nota máxima é obrigatória")
        @Positive(message = "Nota máxima deve ser maior que zero")
        @DecimalMax(value = "1000.0", message = "Nota máxima deve ser no máximo 1000")
        Double notaMaxima,

        @Positive(message = "Peso deve ser maior que zero")
        @DecimalMax(value = "100.0", message = "Peso deve ser no máximo 100")
        Double peso, // opcional — obrigatoriedade validada no service conforme tipoMedia

        @NotNull(message = "Disciplina é obrigatória")
        UUID idDisciplina
) {}