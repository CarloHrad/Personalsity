package com.example.trinots.repository;

import com.example.trinots.domain.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {
    List<Horario> findByDisciplinaIdDisciplina(UUID idDisciplina);
}