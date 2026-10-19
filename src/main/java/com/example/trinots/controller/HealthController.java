package com.example.trinots.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/** Health check publico para o Railway: 200 se o banco responde, 503 se nao. Nao expoe detalhes. */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try (Connection conexao = dataSource.getConnection()) {
            if (conexao.isValid(2)) {
                return ResponseEntity.ok(Map.of("status", "UP"));
            }
        } catch (Exception e) {
            log.warn("Health check: banco indisponivel ({})", e.getClass().getSimpleName());
        }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }
}
