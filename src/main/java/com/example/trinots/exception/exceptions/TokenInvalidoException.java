package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class TokenInvalidoException extends NegocioException {
    public TokenInvalidoException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "430");
    }
}