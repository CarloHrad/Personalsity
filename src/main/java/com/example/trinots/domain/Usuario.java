package com.example.trinots.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


import java.time.LocalDate;
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

    private String email;

    private String senha;

    @ManyToOne
    @JoinColumn(name = "id_curso")
    private Curso curso;

    private Integer semestreAtual;

    private boolean ativo = true;

    private Double mediaAprovacao = 6.0;
}
