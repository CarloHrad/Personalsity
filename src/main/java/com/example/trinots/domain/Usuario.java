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
    @JoinColumn(name = "idUsuario")
    private Curso curso;

    private Integer semestreAtual;

    private LocalDate dataNascimento;

    private boolean ativo = true;
}
