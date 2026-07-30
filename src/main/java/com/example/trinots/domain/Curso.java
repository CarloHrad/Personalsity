package com.example.trinots.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "curso")
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idCurso;

    private String nomeCurso;

    private String instituicao;

    private Integer duracao;
}
