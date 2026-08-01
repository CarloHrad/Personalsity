package com.example.trinots.dto.AuthDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 4, max = 32, message = "Nova senha deve ter entre 4 e 32 caracteres")
        String novaSenha,

        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 4, max = 32, message = "Nova senha deve ter entre 4 e 32 caracteres")
        String novaSenhaConfirmada
) {}