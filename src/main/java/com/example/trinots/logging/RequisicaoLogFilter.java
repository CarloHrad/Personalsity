package com.example.trinots.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Primeiro filtro da cadeia (antes do Spring Security). Para cada requisicao:
 * - define um requestId (do header X-Request-Id, se seguro, ou novo), coloca no MDC e devolve no response;
 * - ao final registra UMA linha: "METODO /caminho STATUS 12ms ip=... user=...".
 * 401/403 saem como WARN e 5xx como ERROR. O /health bem-sucedido nao e registrado (ruido do healthcheck).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequisicaoLogFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    /** Preenchido pelo SecurityFilter quando o token e valido. */
    public static final String ATRIBUTO_USUARIO_ID = "trinots.usuarioId";

    private static final Logger log = LoggerFactory.getLogger(RequisicaoLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestId = RequisicaoLogs.requestIdValido(request.getHeader(HEADER_REQUEST_ID));
        if (requestId == null) {
            requestId = RequisicaoLogs.novoRequestId();
        }
        MDC.put(RequisicaoLogs.MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        long inicio = System.nanoTime();
        boolean falhou = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException e) {
            falhou = true;
            throw e;
        } finally {
            try {
                registrar(request, falhou ? 500 : response.getStatus(), (System.nanoTime() - inicio) / 1_000_000);
            } finally {
                MDC.remove(RequisicaoLogs.MDC_REQUEST_ID);
            }
        }
    }

    private void registrar(HttpServletRequest request, int status, long ms) {
        String caminho = request.getRequestURI();
        if (status < 400 && "/health".equals(caminho)) {
            return;
        }
        String ip = RequisicaoLogs.ipDoCliente(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
        String linha = RequisicaoLogs.linha(request.getMethod(), caminho, status, ms, ip,
                request.getAttribute(ATRIBUTO_USUARIO_ID));
        switch (RequisicaoLogs.nivelPara(status)) {
            case ERROR -> log.error(linha);
            case WARN -> log.warn(linha);
            default -> log.info(linha);
        }
    }
}
