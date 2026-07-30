package com.example.trinots.dto.AvaliacaoDTO;

import com.example.trinots.domain.enums.TipoAvaliacaoEnum;

import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoResponseDTO(
        UUID idAvaliacao,
        String nomeAvaliacao,
        String descricao,
        TipoAvaliacaoEnum tipoAvaliacao,
        LocalDate data,
        Double notaObtida,
        Double notaMaxima,
        Double peso,
        Boolean concluida,
        String nomeDisciplina
) {}
