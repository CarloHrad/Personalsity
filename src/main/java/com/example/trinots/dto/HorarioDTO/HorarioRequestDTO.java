package com.example.trinots.dto.HorarioDTO;

import com.example.trinots.domain.enums.DiaSemanaEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioRequestDTO(
        @NotNull(message = "Dia da semana é obrigatório")
        DiaSemanaEnum diaSemana,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        @NotNull(message = "Hora de fim é obrigatória")
        LocalTime horaFim,

        @NotNull(message = "Disciplina é obrigatória")
        UUID idDisciplina
) {
    @AssertTrue(message = "Hora de fim deve ser depois da hora de início")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
    }
}