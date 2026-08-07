package com.example.trinots.repository;

import com.example.trinots.domain.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {
    List<Tarefa> findByDisciplinaIdDisciplina(UUID idDisciplina);
    Optional<Tarefa> findByIdTarefaAndDisciplinaUsuarioIdUsuario(UUID idTarefa, UUID idUsuario);
    List<Tarefa> findByDisciplinaUsuarioIdUsuario(UUID idUsuario);
    List<Tarefa> findByDisciplinaUsuarioIdUsuarioAndDisciplinaPeriodo(UUID idUsuario, Integer periodo);
}