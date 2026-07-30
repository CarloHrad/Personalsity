package com.example.trinots.domain;

import com.example.trinots.domain.enums.TipoTarefaEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    private TipoTarefaEnum tipoTarefa;

    private LocalDate dataEntrega;

    private LocalDate dataConclusao;

    private Boolean concluida = false;

    @ManyToOne
    @JoinColumn(name = "idDisciplina")
    private Disciplina disciplina;

}
