package com.example.trinots.dto.AuthDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaComTokenDTO(
        @NotBlank(message = "Token é obrigatório")
        String token,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 4, max = 32, message = "Senha deve ter entre 4 e 32 caracteres")
        String novaSenha
) {}