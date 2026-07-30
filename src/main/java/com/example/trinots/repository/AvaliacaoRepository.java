package com.example.trinots.repository;

import com.example.trinots.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {
    List<Avaliacao> findByDisciplinaIdDisciplina(UUID idDisciplina);
}