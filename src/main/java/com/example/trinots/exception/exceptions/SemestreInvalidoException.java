package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class SemestreInvalidoException extends NegocioException {
    public SemestreInvalidoException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "476");
    }
}