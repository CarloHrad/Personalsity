package com.example.trinots.domain;

import jakarta.persistence.*;
import lombok.*;


import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idUsuario;

    private String nome;

    private String sobrenome;

    @Column(unique = true, nullable = false)
    private String email;

    private String senha;

    @ManyToOne
    @JoinColumn(name = "id_curso")
    private Curso curso;

    private Integer semestreAtual;

    private Double mediaAprovacao = 6.0;

    private boolean ativo = true;

    private boolean emailVerificado = false;
}
