package com.example.trinots.dto.UsuarioDTO;

import com.example.trinots.dto.CursoDTO.CursoResponseDTO;

import java.time.LocalDate;
import java.util.UUID;

public record UsuarioResponseDTO(
        UUID idUsuario,
        String nome,
        String sobrenome,
        String email,
        CursoResponseDTO curso,
        Integer semestreAtual,
        boolean ativo
) {}
