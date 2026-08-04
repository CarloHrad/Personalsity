package com.example.trinots.dto.HorarioDTO;

import com.example.trinots.domain.enums.DiaSemanaEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalTime;

public record HorarioEmbutidoDTO(
        @NotNull(message = "Dia da semana é obrigatório")
        DiaSemanaEnum diaSemana,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        @NotNull(message = "Hora de fim é obrigatória")
        LocalTime horaFim
) {
    @AssertTrue(message = "Hora de fim deve ser depois da hora de início")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFim == null || horaFim.isAfter(horaInicio);
    }

    @AssertTrue(message = "Horário deve estar entre 06:00 e 23:59")
    public boolean isDentroDoIntervaloPermitido() {
        if (horaInicio == null || horaFim == null) return true;
        return !horaInicio.isBefore(LocalTime.of(6, 0)) && !horaFim.isAfter(LocalTime.of(23, 59));
    }

    @AssertTrue(message = "A aula deve durar pelo menos 15 minutos")
    public boolean isDuracaoMinimaValida() {
        if (horaInicio == null || horaFim == null || !horaFim.isAfter(horaInicio)) return true;
        return Duration.between(horaInicio, horaFim).toMinutes() >= 15;
    }

    @AssertTrue(message = "A aula não pode durar mais de 8 horas")
    public boolean isDuracaoMaximaValida() {
        if (horaInicio == null || horaFim == null || !horaFim.isAfter(horaInicio)) return true;
        return Duration.between(horaInicio, horaFim).toMinutes() <= 480;
    }
}