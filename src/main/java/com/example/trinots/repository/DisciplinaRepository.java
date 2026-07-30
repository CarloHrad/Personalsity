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

    Optional<Disciplina> findByNomeDisciplina(String nomeDisciplina);

    boolean existsByNomeDisciplina(String nomeDisciplina);

    List<Disciplina> findByStatus(StatusDisciplinaEnum status);

    List<Disciplina> findByArquivadaFalse();

}