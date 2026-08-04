package com.example.trinots.dto.AvaliacaoDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AvaliacaoResponseDTO(
        UUID idAvaliacao,
        String nomeAvaliacao,
        String descricao,
        LocalDate dataAvaliacao,
        LocalDateTime dataConclusao,
        Double notaObtida,
        Double notaMaxima,
        Double peso,
        Boolean concluida,
        String nomeDisciplina
) {}
