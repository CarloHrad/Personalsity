package com.example.trinots.dto.CursoDTO;

import java.util.UUID;

public record CursoResponseDTO(
        UUID idCurso,
        String nomeCurso,
        String instituicao,
        Integer duracao
) {}