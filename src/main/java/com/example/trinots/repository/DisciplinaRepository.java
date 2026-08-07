package com.example.trinots.repository;

import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisciplinaRepository extends JpaRepository<Disciplina, UUID> {

    List<Disciplina> findByUsuarioIdUsuario(UUID idUsuario);

    boolean existsByNomeDisciplinaAndUsuarioIdUsuario(String nomeDisciplina, UUID idUsuario);

    Optional<Disciplina> findByIdDisciplinaAndUsuarioIdUsuario(UUID idDisciplina, UUID idUsuario);

    List<Disciplina> findByUsuarioIdUsuarioAndPeriodo(UUID idUsuario, Integer periodo);

    List<Disciplina> findByUsuarioIdUsuarioAndPeriodoAndStatus(UUID idUsuario, Integer periodo, StatusDisciplinaEnum status);
    
    List<Disciplina> findByUsuarioIdUsuarioAndArquivadaTrue(UUID idUsuario);

    List<Disciplina> findByUsuarioIdUsuarioAndPeriodoAndArquivadaTrue(UUID idUsuario, Integer periodo);

}