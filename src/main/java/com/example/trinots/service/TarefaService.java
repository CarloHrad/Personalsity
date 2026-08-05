package com.example.trinots.service;

import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.Tarefa;
import com.example.trinots.domain.enums.StatusTarefaEnum;
import com.example.trinots.dto.TarefaDTO.TarefaRequestDTO;
import com.example.trinots.dto.TarefaDTO.TarefaResponseDTO;
import com.example.trinots.exception.exceptions.DisciplinaArquivadaException;
import com.example.trinots.exception.exceptions.TarefaJaConcluidaException;
import com.example.trinots.exception.exceptions.TarefaNaoConcluidaException;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.repository.TarefaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final DisciplinaRepository disciplinaRepository;

    public TarefaService(TarefaRepository tarefaRepository, DisciplinaRepository disciplinaRepository) {
        this.tarefaRepository = tarefaRepository;
        this.disciplinaRepository = disciplinaRepository;
    }

    public TarefaResponseDTO criarTarefa(TarefaRequestDTO dto, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível adicionar tarefa a uma disciplina arquivada");
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setNomeTarefa(dto.nomeTarefa());
        tarefa.setDescricao(dto.descricao());
        tarefa.setDataEntrega(dto.dataEntrega());
        tarefa.setConcluida(false);
        tarefa.setDataConclusao(null);
        tarefa.setDisciplina(disciplina);

        Tarefa salva = tarefaRepository.save(tarefa);
        return toResponseDTO(salva);
    }


    public TarefaResponseDTO buscarTarefaPorId(UUID id, UUID idUsuarioLogado) {
        Tarefa tarefa = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        return toResponseDTO(tarefa);
    }


    public List<TarefaResponseDTO> listarPorDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        buscarDisciplinaDoUsuario(idDisciplina, idUsuarioLogado);
        return tarefaRepository.findByDisciplinaIdDisciplina(idDisciplina)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }


    public List<TarefaResponseDTO> listarPendentes(UUID idUsuarioLogado) {
        return tarefaRepository.findByConcluidaFalseAndDisciplinaUsuarioIdUsuario(idUsuarioLogado)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }


    public TarefaResponseDTO atualizarTarefa(UUID id, TarefaRequestDTO dto, UUID idUsuarioLogado) {
        Tarefa tarefa = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        Disciplina disciplina = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível editar tarefa de uma disciplina arquivada");
        }

        if (tarefa.getConcluida()) {
            throw new TarefaJaConcluidaException("Não é possível editar uma tarefa já concluída");
        }

        tarefa.setNomeTarefa(dto.nomeTarefa());
        tarefa.setDescricao(dto.descricao());
        tarefa.setDataEntrega(dto.dataEntrega());
        tarefa.setDisciplina(disciplina);

        Tarefa atualizada = tarefaRepository.save(tarefa);
        return toResponseDTO(atualizada);
    }


    public TarefaResponseDTO concluirTarefa(UUID id, UUID idUsuarioLogado) {
        Tarefa tarefa = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (tarefa.getConcluida()) {
            throw new TarefaJaConcluidaException("Tarefa já está concluída");
        }

        tarefa.setConcluida(true);
        tarefa.setDataConclusao(LocalDateTime.now()); // agora com hora exata

        Tarefa atualizada = tarefaRepository.save(tarefa);
        return toResponseDTO(atualizada);
    }

    public TarefaResponseDTO reabrirTarefa(UUID id, UUID idUsuarioLogado) {
        Tarefa tarefa = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (!tarefa.getConcluida()) {
            throw new TarefaNaoConcluidaException("Tarefa ainda não foi concluída");
        }

        tarefa.setConcluida(false);
        tarefa.setDataConclusao(null);

        Tarefa atualizada = tarefaRepository.save(tarefa);
        return toResponseDTO(atualizada);
    }


    public void deletarTarefa(UUID id, UUID idUsuarioLogado) {
        Tarefa tarefa = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        tarefaRepository.delete(tarefa);
    }


    private Disciplina buscarDisciplinaDoUsuario(UUID idDisciplina, UUID idUsuarioLogado) {
        return disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada"));
    }


    private Tarefa buscarEntidadeDoUsuario(UUID id, UUID idUsuarioLogado) {
        return tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(id, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));
    }

    private StatusTarefaEnum calcularStatus(Tarefa tarefa) {

        if (Boolean.TRUE.equals(tarefa.getConcluida())) {
            return StatusTarefaEnum.CONCLUIDA;
        }

        if (tarefa.getDataEntrega().isBefore(LocalDate.now())) {
            return StatusTarefaEnum.ATRASADA;
        }

        return StatusTarefaEnum.PENDENTE;
    }


    private TarefaResponseDTO toResponseDTO(Tarefa tarefa) {
        return new TarefaResponseDTO(
                tarefa.getIdTarefa(),
                tarefa.getNomeTarefa(),
                tarefa.getDescricao(),
                tarefa.getDataEntrega(),
                tarefa.getDataConclusao(),
                calcularStatus(tarefa),
                tarefa.getDisciplina().getNomeDisciplina()
        );
    }
}
