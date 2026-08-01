package com.example.trinots.exception.exceptions;


import org.springframework.http.HttpStatus;

public class TipoMediaIndefinidaException extends NegocioException {
    public TipoMediaIndefinidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "470");
    }
}
