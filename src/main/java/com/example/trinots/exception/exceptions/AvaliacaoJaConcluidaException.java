package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class AvaliacaoJaConcluidaException extends NegocioException {
    public AvaliacaoJaConcluidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "474");
    }
}