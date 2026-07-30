package com.example.trinots.dto.DisciplinaDTO;

import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import com.example.trinots.domain.enums.TipoMediaEnum;
import jakarta.validation.constraints.*;

public record DisciplinaRequestDTO(
        @NotBlank(message = "Nome da disciplina é obrigatório")
        @Size(min = 2, max = 150, message = "Nome da disciplina deve ter entre 2 e 150 caracteres")
        String nomeDisciplina,

        @NotNull(message = "Período é obrigatório")
        @Min(value = 1, message = "Período deve ser no mínimo 1")
        @Max(value = 20, message = "Período deve ser no máximo 20")
        Integer periodo,

        @Size(max = 100, message = "Nome do professor deve ter no máximo 100 caracteres")
        String professor,

        @Size(max = 20, message = "Sala deve ter no máximo 20 caracteres")
        String sala,

        @Min(value = 0, message = "Andar não pode ser negativo")
        @Max(value = 200, message = "Andar deve ser no máximo 200")
        Integer andar,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar em formato hexadecimal (ex: #FF5733)")
        String cor,

        TipoMediaEnum tipoMedia
) {}