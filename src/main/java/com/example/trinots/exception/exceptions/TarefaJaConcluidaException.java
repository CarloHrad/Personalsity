package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class TarefaJaConcluidaException extends NegocioException {
    public TarefaJaConcluidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "460");
    }
}