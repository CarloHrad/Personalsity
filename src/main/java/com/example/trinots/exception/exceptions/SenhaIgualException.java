package com.example.trinots.exception.exceptions;

import org.springframework.http.HttpStatus;

public class SenhaIgualException extends NegocioException {
    public SenhaIgualException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "477");
    }
}
