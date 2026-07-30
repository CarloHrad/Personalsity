package com.example.trinots.dto.UsuarioDTO;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record UsuarioUpdateDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotBlank(message = "Sobrenome é obrigatório")
        @Size(min = 2, max = 100, message = "Sobrenome deve ter entre 2 e 100 caracteres")
        String sobrenome,

        @NotNull(message = "Semestre é obrigatório")
        @Min(value = 1, message = "Semestre deve ser no mínimo 1")
        @Max(value = 20, message = "Semestre deve ser no máximo 20")
        Integer semestreAtual
) {}
