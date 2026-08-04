package com.example.trinots.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "avaliacao")
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idAvaliacao;

    private String nomeAvaliacao;

    private String descricao;

    private LocalDate dataAvaliacao;

    private LocalDateTime dataConclusao;

    private Double notaObtida;

    private Double notaMaxima;

    private Double peso;

    private Boolean concluida = false;

    @ManyToOne
    @JoinColumn(name = "id_disciplina")
    private Disciplina disciplina;

}
