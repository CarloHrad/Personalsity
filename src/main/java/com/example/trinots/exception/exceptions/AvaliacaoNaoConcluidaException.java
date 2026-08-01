package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class AvaliacaoNaoConcluidaException extends NegocioException {
    public AvaliacaoNaoConcluidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "475");
    }
}