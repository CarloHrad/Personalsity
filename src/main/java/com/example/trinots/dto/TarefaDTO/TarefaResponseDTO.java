package com.example.trinots.dto.TarefaDTO;

import com.example.trinots.domain.enums.TipoTarefaEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TarefaResponseDTO(
        UUID idTarefa,
        String nomeTarefa,
        String descricao,
        TipoTarefaEnum tipoTarefa,
        LocalDate dataEntrega,
        LocalDateTime dataConclusao,
        Boolean concluida,
        String nomeDisciplina
) {}
