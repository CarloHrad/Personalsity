package com.example.trinots.dto.DisciplinaDTO;

import com.example.trinots.domain.Avaliacao;
import com.example.trinots.domain.Tarefa;
import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.HorarioDTO.HorarioResponseDTO;

import java.util.List;
import java.util.UUID;

public record DisciplinaResponseDTO(
        UUID idDisciplina,
        String nomeDisciplina,
        Integer periodo,
        String professor,
        String sala,
        Integer andar,
        String cor,
        StatusDisciplinaEnum status,
        TipoMediaEnum tipoMedia,
        Double mediaAtual,
        Double faltaParaAprovacao,
        Boolean arquivada,
        List<HorarioResponseDTO> horarios
) {}
