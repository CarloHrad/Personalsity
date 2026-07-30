package com.example.trinots.exception.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class NegocioException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    protected NegocioException(String message, HttpStatus status, String codigo) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

}