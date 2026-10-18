package com.example.trinots.dto.UsuarioDTO;

import jakarta.validation.constraints.NotBlank;

public record ExcluirContaDTO(
        @NotBlank(message = "Senha é obrigatória para excluir a conta")
        String senha
) {}
