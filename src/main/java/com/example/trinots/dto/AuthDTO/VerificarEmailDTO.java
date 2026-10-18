package com.example.trinots.dto.AuthDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarEmailDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Código é obrigatório")
        @Pattern(regexp = "^\\d{6}$", message = "Código deve ter 6 dígitos")
        String codigo
) {}
