package com.example.trinots.repository;

import com.example.trinots.domain.Horario;
import com.example.trinots.domain.enums.DiaSemanaEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {
    List<Horario> findByDisciplinaIdDisciplina(UUID idDisciplina);
    List<Horario> findByDisciplinaUsuarioIdUsuarioAndDiaSemana(UUID idUsuario, DiaSemanaEnum diaSemana);
    Optional<Horario> findByIdHoraAndDisciplinaUsuarioIdUsuario(UUID idHora, UUID idUsuario);
}