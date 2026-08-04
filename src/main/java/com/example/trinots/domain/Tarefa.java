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
@Table(name = "tarefa")
public class Tarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idTarefa;

    private String nomeTarefa;

    private String descricao;

    private LocalDate dataEntrega;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    private Boolean concluida = false;

    @ManyToOne
    @JoinColumn(name = "id_disciplina")
    private Disciplina disciplina;

}
