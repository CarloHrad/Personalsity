package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class DisciplinaImutavelException extends NegocioException {
    public DisciplinaImutavelException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "443");
    }
}