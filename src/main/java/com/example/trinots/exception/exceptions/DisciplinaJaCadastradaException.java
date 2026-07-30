package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class DisciplinaJaCadastradaException extends NegocioException {
    public DisciplinaJaCadastradaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "440");
    }
}