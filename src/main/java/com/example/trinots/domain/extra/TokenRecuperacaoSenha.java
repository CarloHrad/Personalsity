package com.example.trinots.domain.extra;

import com.example.trinots.domain.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idToken;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String token;

    private Instant dataExpiracao;

    private boolean usado = false;
}