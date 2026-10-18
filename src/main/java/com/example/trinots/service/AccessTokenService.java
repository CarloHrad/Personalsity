package com.example.trinots.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.trinots.domain.Usuario;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccessTokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    /**
     * HMAC256 exige chave de pelo menos 32 bytes. Falha na subida (em vez de rodar com segredo fraco),
     * obrigando a definir a variavel de ambiente JWT_SECRET em producao.
     */
    @PostConstruct
    void validarSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "api.security.token.secret deve ter pelo menos 32 bytes. Defina a variavel de ambiente JWT_SECRET.");
        }
    }

    public String generateToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create().withIssuer("auth").withSubject(usuario.getEmail())
                    .withJWTId(UUID.randomUUID().toString())
                    .withExpiresAt(genExpirationDate()).sign(algorithm);
            return token;
        } catch (JWTCreationException jwtCreationException) {
            throw new RuntimeException("Erro na geração do token", jwtCreationException);
        }
    }

    public String validateToken(String token) {
        DecodedJWT decodificado = decodificar(token);
        return decodificado == null ? null : decodificado.getSubject();
    }

    public DecodedJWT decodificar(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm).withIssuer("auth").build().verify(token);
        } catch (JWTVerificationException jwtVerificationException) {
            return null;
        }
    }

    private Instant genExpirationDate(){
        return Instant.now().plusSeconds(7200);
    }


}
