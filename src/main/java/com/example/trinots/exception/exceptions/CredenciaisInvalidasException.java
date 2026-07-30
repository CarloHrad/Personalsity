package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class CredenciaisInvalidasException extends NegocioException {
    public CredenciaisInvalidasException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "401");
    }
}