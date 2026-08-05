package com.example.trinots.dto.TarefaDTO;

import com.example.trinots.domain.enums.StatusTarefaEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TarefaResponseDTO(
        UUID idTarefa,
        String nomeTarefa,
        String descricao,
        LocalDate dataEntrega,
        LocalDateTime dataConclusao,
        StatusTarefaEnum status,
        String nomeDisciplina
) {}
