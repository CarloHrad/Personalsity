package com.example.trinots.exception.exceptions;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorValidationResponseDTO(
    String codigo,
    String mensagem,
    LocalDateTime timestamp,
    Map<String,
    String> erros
) {}