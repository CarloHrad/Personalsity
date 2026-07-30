package com.example.trinots.dto.TarefaDTO;

import com.example.trinots.domain.enums.TipoTarefaEnum;

import java.time.LocalDate;
import java.util.UUID;

public record TarefaResponseDTO(
        UUID idTarefa,
        String nomeTarefa,
        String descricao,
        TipoTarefaEnum tipoTarefa,
        LocalDate dataEntrega,
        LocalDate dataConclusao,
        Boolean concluida,
        String nomeDisciplina
) {}
