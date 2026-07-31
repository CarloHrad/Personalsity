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

        validarSemConflito(dto.diaSemana(), dto.horaInicio(), dto.horaFim(), idUsuarioLogado, null);

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
        buscarDisciplinaDoUsuario(idDisciplina, idUsuarioLogado); // garante que a disciplina é do usuário
        return horarioRepository.findByDisciplinaIdDisciplina(idDisciplina)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }


    public HorarioResponseDTO atualizarHorario(UUID id, HorarioRequestDTO dto, UUID idUsuarioLogado) {
        Horario horario = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        Disciplina disciplina = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível editar horário de uma disciplina arquivada");
        }

        // exclui o próprio registro da checagem de conflito, senão ele bateria com ele mesmo
        validarSemConflito(dto.diaSemana(), dto.horaInicio(), dto.horaFim(), idUsuarioLogado, id);

        horario.setDiaSemana(dto.diaSemana());
        horario.setHoraInicio(dto.horaInicio());
        horario.setHoraFim(dto.horaFim());
        horario.setDisciplina(disciplina);

        Horario atualizado = horarioRepository.save(horario);
        return toResponseDTO(atualizado);
    }


    public void deletarHorario(UUID id, UUID idUsuarioLogado) {
        Horario horario = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        horarioRepository.delete(horario);
    }



    private void validarSemConflito(DiaSemanaEnum diaSemana, LocalTime horaInicio, LocalTime horaFim,
                                    UUID idUsuarioLogado, UUID idHorarioIgnorar) {
        List<Horario> horariosDoMesmoDia = horarioRepository
                .findByDisciplinaUsuarioIdUsuarioAndDiaSemana(idUsuarioLogado, diaSemana);

        boolean temConflito = horariosDoMesmoDia.stream()
                .filter(h -> !h.getIdHora().equals(idHorarioIgnorar)) // ignora ele mesmo, em caso de update
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