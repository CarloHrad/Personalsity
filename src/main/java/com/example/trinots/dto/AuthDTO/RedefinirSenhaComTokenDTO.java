package com.example.trinots.dto.AuthDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaComTokenDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Código é obrigatório")
        @Pattern(regexp = "^\\d{6}$", message = "Código deve ter 6 dígitos")
        String codigo,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 4, max = 32, message = "Senha deve ter entre 4 e 32 caracteres")
        String novaSenha
) {}
