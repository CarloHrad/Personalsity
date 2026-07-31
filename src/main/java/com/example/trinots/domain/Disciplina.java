package com.example.trinots.domain;

import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import com.example.trinots.domain.enums.TipoMediaEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "disciplina")
public class Disciplina {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idDisciplina;

    private String nomeDisciplina;

    private Integer periodo;

    private String professor;

    private String sala;

    private Integer andar;

    private String cor;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL)
    private List<Horario> horarios;

    @Enumerated(EnumType.STRING)
    private StatusDisciplinaEnum status;

    @Enumerated(EnumType.STRING)
    private TipoMediaEnum tipoMedia;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private Boolean arquivada = false;

    @OneToMany(mappedBy = "disciplina")
    private List<Avaliacao> avaliacoes;

    @OneToMany(mappedBy = "disciplina")
    private List<Tarefa> tarefas;

}
