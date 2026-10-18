package com.example.trinots.service;

import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.Horario;
import com.example.trinots.domain.enums.DiaSemanaEnum;
import com.example.trinots.dto.HorarioDTO.HorarioRequestDTO;
import com.example.trinots.dto.HorarioDTO.HorarioResponseDTO;
import com.example.trinots.exception.exceptions.DisciplinaArquivadaException;
import com.example.trinots.exception.exceptions.HorarioConflitanteException;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.repository.HorarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final DisciplinaRepository disciplinaRepository;

    public HorarioService(HorarioRepository horarioRepository, DisciplinaRepository disciplinaRepository) {
        this.horarioRepository = horarioRepository;
        this.disciplinaRepository = disciplinaRepository;
    }

    public HorarioResponseDTO criarHorario(HorarioRequestDTO dto, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível adicionar horário a uma disciplina arquivada");
        }

        validarSemConflito(dto.diaSemana(), dto.horaInicio(), dto.horaFim(), idUsuarioLogado, disciplina.getPeriodo(), null);

        Horario horario = new Horario();
        horario.setDiaSemana(dto.diaSemana());
        horario.setHoraInicio(dto.horaInicio());
        horario.setHoraFim(dto.horaFim());
        horario.setDisciplina(disciplina);

        Horario salvo = horarioRepository.save(horario);
        return toResponseDTO(salvo);
    }

    public HorarioResponseDTO buscarHorarioPorId(UUID id, UUID idUsuarioLogado) {
        Horario horario = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        return toResponseDTO(horario);
    }

    public List<HorarioResponseDTO> listarPorDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        buscarDisciplinaDoUsuario(idDisciplina, idUsuarioLogado);
        return horarioRepository.findByDisciplinaIdDisciplina(idDisciplina)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public HorarioResponseDTO atualizarHorario(UUID id, HorarioRequestDTO dto, UUID idUsuarioLogado) {
        Horario horario = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (horario.getDisciplina().getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível editar horário de uma disciplina arquivada");
        }

        Disciplina disciplinaDestino = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplinaDestino.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível mover horário para uma disciplina arquivada");
        }

        validarSemConflito(dto.diaSemana(), dto.horaInicio(), dto.horaFim(), idUsuarioLogado, disciplinaDestino.getPeriodo(), id);

        horario.setDiaSemana(dto.diaSemana());
        horario.setHoraInicio(dto.horaInicio());
        horario.setHoraFim(dto.horaFim());
        horario.setDisciplina(disciplinaDestino);

        Horario atualizado = horarioRepository.save(horario);
        return toResponseDTO(atualizado);
    }

    public void deletarHorario(UUID id, UUID idUsuarioLogado) {
        Horario horario = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        horarioRepository.delete(horario);
    }

    /**
     * Revalida todos os horários de uma disciplina contra as demais disciplinas ativas do mesmo período.
     * Usado ao desarquivar, já que horários de disciplinas arquivadas não entram na checagem de conflito.
     */
    public void validarHorariosDaDisciplina(Disciplina disciplina, UUID idUsuarioLogado) {
        for (Horario horario : horarioRepository.findByDisciplinaIdDisciplina(disciplina.getIdDisciplina())) {
            validarSemConflito(horario.getDiaSemana(), horario.getHoraInicio(), horario.getHoraFim(),
                    idUsuarioLogado, disciplina.getPeriodo(), horario.getIdHora());
        }
    }

    private void validarSemConflito(DiaSemanaEnum diaSemana, LocalTime horaInicio, LocalTime horaFim,
                                    UUID idUsuarioLogado, Integer periodo, UUID idHorarioIgnorar) {
        List<Horario> horariosDoMesmoDia = horarioRepository
                .findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, diaSemana);

        boolean temConflito = horariosDoMesmoDia.stream()
                .filter(h -> Objects.equals(h.getDisciplina().getPeriodo(), periodo))
                .filter(h -> !h.getIdHora().equals(idHorarioIgnorar))
                .anyMatch(h -> horaInicio.isBefore(h.getHoraFim()) && horaFim.isAfter(h.getHoraInicio()));

        if (temConflito) {
            throw new HorarioConflitanteException(
                    "Já existe um horário cadastrado em " + diaSemana + " que conflita com o intervalo informado");
        }
    }

    private Disciplina buscarDisciplinaDoUsuario(UUID idDisciplina, UUID idUsuarioLogado) {
        return disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada"));
    }

    private Horario buscarEntidadeDoUsuario(UUID id, UUID idUsuarioLogado) {
        return horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(id, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Horário não encontrado"));
    }

    private HorarioResponseDTO toResponseDTO(Horario horario) {
        return new HorarioResponseDTO(
                horario.getIdHora(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFim()
        );
    }
}