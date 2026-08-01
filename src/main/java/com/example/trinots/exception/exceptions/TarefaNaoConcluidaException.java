package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class TarefaNaoConcluidaException extends NegocioException {
    public TarefaNaoConcluidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "461");
    }
}