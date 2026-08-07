package com.example.trinots.dto.MediaDTO;

import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoMediaResponseDTO;

import java.util.List;

public record MediaResponseDTO(
        TipoMediaEnum tipoMedia,
        Double mediaAtual,
        Double mediaAprovacao,
        Double faltaParaAprovacao,
        List<AvaliacaoMediaResponseDTO> avaliacoes
) {}