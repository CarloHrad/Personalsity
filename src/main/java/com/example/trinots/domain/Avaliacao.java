package com.example.trinots.domain;

import com.example.trinots.domain.enums.TipoAvaliacaoEnum;
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
@Table(name = "avaliacao")
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idAvaliacao;

    private String nomeAvaliacao;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private TipoAvaliacaoEnum tipoAvaliacao;

    private LocalDate data;

    private Double notaObtida;

    private Double notaMaxima;

    private Double peso;

    private Boolean concluida = false;

    @ManyToOne
    @JoinColumn(name = "idDisciplina")
    private Disciplina disciplina;

}
