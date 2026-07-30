package com.example.trinots.dto.HorarioDTO;

import com.example.trinots.domain.enums.DiaSemanaEnum;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioResponseDTO(
        UUID idHora,
        DiaSemanaEnum diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim
) {}
