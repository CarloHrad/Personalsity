package com.example.trinots.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.trinots.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AccessTokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create().withIssuer("auth").withSubject(usuario.getEmail())
                    .withExpiresAt(genExpirationDate()).sign(algorithm);
            return token;
        } catch (JWTCreationException jwtCreationException) {
            throw new RuntimeException("Erro na geração do token", jwtCreationException);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm).withIssuer("auth").build().verify(token).getSubject();
        } catch (JWTVerificationException jwtVerificationException) {
            return null;
        }
    }

    private Instant genExpirationDate(){
        return Instant.now().plusSeconds(7200);//2horas fela
    }


}
