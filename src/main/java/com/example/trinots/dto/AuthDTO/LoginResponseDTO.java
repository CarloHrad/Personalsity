package com.example.trinots.dto.AuthDTO;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        UUID idUsuario,
        String nome
) {}