package com.example.trinots.dto.UsuarioDTO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record NovaMediaDTO(
        @NotNull(message = "A nova média mínima é obrigatória")
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        Double novaMedia
) {}
