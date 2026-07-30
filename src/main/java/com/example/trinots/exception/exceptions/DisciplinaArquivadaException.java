package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class DisciplinaArquivadaException extends NegocioException {
    public DisciplinaArquivadaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "410");
    }
}