package com.example.trinots.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.trinots.domain.Usuario;
import com.example.trinots.repository.TokenRevogadoRepository;
import com.example.trinots.repository.UsuarioRepository;
import com.example.trinots.service.AccessTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final AccessTokenService accessTokenService;
    private final UsuarioRepository usuarioRepository;
    private final TokenRevogadoRepository tokenRevogadoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extrairToken(request);

        if (token != null) {
            DecodedJWT jwt = accessTokenService.decodificar(token);

            if (jwt != null && jwt.getId() != null && !tokenRevogadoRepository.existsById(jwt.getId())) {
                Usuario usuario = usuarioRepository.findByEmail(jwt.getSubject())
                        .filter(Usuario::isAtivo)
                        .orElse(null);

                if (usuario != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length());
    }
}
