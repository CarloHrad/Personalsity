package com.example.trinots.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Monta o corpo JSON de erro dos handlers de seguranca (401/403) sem depender do ObjectMapper.
 *
 * Por que existe: o ObjectMapper criado com "new ObjectMapper()" nao sabe serializar LocalDateTime
 * (falta o modulo jackson-datatype-jsr310), o que fazia o 401 virar HTTP 500
 * ("Java 8 date/time type java.time.LocalDateTime not supported by default").
 * O formato e identico ao ErrorResponseDTO: {"codigo","mensagem","timestamp"}.
 */
final class RespostaErroJson {

    private RespostaErroJson() {
    }

    static String montar(String codigo, String mensagem, LocalDateTime timestamp) {
        return "{\"codigo\":" + texto(codigo)
                + ",\"mensagem\":" + texto(mensagem)
                + ",\"timestamp\":" + texto(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                + "}";
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
