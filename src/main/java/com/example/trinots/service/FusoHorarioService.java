package com.example.trinots.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Resolve o fuso horário do aparelho do usuário (RN07).
 *
 * O app deve enviar o header {@code X-Timezone} com o identificador IANA do aparelho
 * (ex.: {@code America/Sao_Paulo}). Sem o header, ou com valor inválido, usa-se {@link #FUSO_PADRAO}.
 */
@Component
public class FusoHorarioService {

    public static final String HEADER = "X-Timezone";
    public static final ZoneId FUSO_PADRAO = ZoneId.of("America/Sao_Paulo");

    public ZoneId fusoAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String valor = atributos.getRequest().getHeader(HEADER);
            if (valor != null && !valor.isBlank()) {
                try {
                    return ZoneId.of(valor.trim());
                } catch (DateTimeException ignorado) {
                }
            }
        }
        return FUSO_PADRAO;
    }

    public LocalDate hoje() {
        return LocalDate.now(fusoAtual());
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(fusoAtual());
    }
}
