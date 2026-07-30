package com.example.trinots.repository;

import com.example.trinots.domain.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {
    List<Tarefa> findByDisciplinaIdDisciplina(UUID idDisciplina);
    List<Tarefa> findByConcluidaFalse();
}