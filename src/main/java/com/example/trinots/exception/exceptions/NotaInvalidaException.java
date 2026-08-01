package com.example.trinots.exception.exceptions;


import org.springframework.http.HttpStatus;

public class NotaInvalidaException extends NegocioException {
    public NotaInvalidaException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "473");
    }
}