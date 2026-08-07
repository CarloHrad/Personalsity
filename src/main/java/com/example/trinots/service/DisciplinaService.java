package com.example.trinots.service;

import com.example.trinots.domain.Avaliacao;
import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.Tarefa;
import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoMediaResponseDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoResponseDTO;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaRequestDTO;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaResponseDTO;
import com.example.trinots.dto.HorarioDTO.HorarioEmbutidoDTO;
import com.example.trinots.dto.HorarioDTO.HorarioRequestDTO;
import com.example.trinots.dto.HorarioDTO.HorarioResponseDTO;
import com.example.trinots.dto.MediaDTO.MediaResponseDTO;
import com.example.trinots.exception.exceptions.*;
import com.example.trinots.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;
    private final HorarioRepository horarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final TarefaRepository tarefaRepository;
    private final HorarioService horarioService;

    private static final double MEDIA_MINIMA_APROVACAO = 6.0;

    public DisciplinaService(DisciplinaRepository disciplinaRepository, AvaliacaoRepository avaliacaoRepository, HorarioRepository horarioRepository, TarefaRepository tarefaRepository, HorarioService horarioService) {
        this.disciplinaRepository = disciplinaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.horarioRepository = horarioRepository;
        this.tarefaRepository = tarefaRepository;
        this.horarioService = horarioService;
    }

    public DisciplinaResponseDTO criarDisciplina(DisciplinaRequestDTO dto, Usuario usuarioLogado) {
        if (disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuario(dto.nomeDisciplina(), usuarioLogado.getIdUsuario())) {
            throw new DisciplinaJaCadastradaException("Você já tem uma disciplina chamada '" + dto.nomeDisciplina() + "'");
        }

        Disciplina disciplina = new Disciplina();
        disciplina.setNomeDisciplina(dto.nomeDisciplina());
        disciplina.setPeriodo(dto.periodo());
        disciplina.setProfessor(dto.professor());
        disciplina.setSala(dto.sala());
        disciplina.setAndar(dto.andar());
        disciplina.setCor(dto.cor());
        disciplina.setStatus(StatusDisciplinaEnum.EM_PROGRESSO);
        disciplina.setTipoMedia(dto.tipoMedia());
        disciplina.setArquivada(false);
        disciplina.setUsuario(usuarioLogado);

        Disciplina salva = disciplinaRepository.save(disciplina);

        if (dto.horarios() != null) {
            for (HorarioEmbutidoDTO h : dto.horarios()) {
                HorarioRequestDTO horarioDto = new HorarioRequestDTO(
                        h.diaSemana(), h.horaInicio(), h.horaFim(), salva.getIdDisciplina());
                horarioService.criarHorario(horarioDto, usuarioLogado.getIdUsuario());
            }
        }

        return toResponseDTO(salva);
    }

    public DisciplinaResponseDTO buscarDisciplinaPorId(UUID id, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        return toResponseDTO(disciplina);
    }

    public List<DisciplinaResponseDTO> listarDisciplinasAtivas(UUID idUsuarioLogado) {
        return disciplinaRepository.findByUsuarioIdUsuario(idUsuarioLogado)
                .stream()
                .filter(d -> !d.getArquivada())
                .map(this::toResponseDTO)
                .toList();
    }

    public List<DisciplinaResponseDTO> listarDisciplinas(Usuario usuario, StatusDisciplinaEnum status) {

        Integer periodoAtual = usuario.getSemestreAtual();

        List<Disciplina> disciplinas = (status == null)
                ? disciplinaRepository.findByUsuarioIdUsuarioAndPeriodo(usuario.getIdUsuario(), periodoAtual)
                : disciplinaRepository.findByUsuarioIdUsuarioAndPeriodoAndStatus(usuario.getIdUsuario(), periodoAtual, status);

        return disciplinas.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<DisciplinaResponseDTO> listarArquivadas(Integer periodo, UUID idUsuarioLogado) {

        List<Disciplina> disciplinas = (periodo != null)
                ? disciplinaRepository.findByUsuarioIdUsuarioAndPeriodoAndArquivadaTrue(idUsuarioLogado, periodo)
                : disciplinaRepository.findByUsuarioIdUsuarioAndArquivadaTrue(idUsuarioLogado);

        return disciplinas.stream().map(this::toResponseDTO).toList();
    }

    public DisciplinaResponseDTO atualizarDisciplina(UUID id, DisciplinaRequestDTO dto, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível editar uma disciplina arquivada");
        }

        boolean temAvaliacoes = !avaliacaoRepository.findByDisciplinaIdDisciplina(id).isEmpty();

        if (temAvaliacoes && !Objects.equals(disciplina.getTipoMedia(), dto.tipoMedia())) {
            throw new TipoMediaImutavelException(
                    "Não é possível alterar o tipo de média: a disciplina já possui avaliações cadastradas");
        }

        disciplina.setNomeDisciplina(dto.nomeDisciplina());
        disciplina.setPeriodo(dto.periodo());
        disciplina.setProfessor(dto.professor());
        disciplina.setSala(dto.sala());
        disciplina.setAndar(dto.andar());
        disciplina.setCor(dto.cor());

        if (!temAvaliacoes) {
            disciplina.setTipoMedia(dto.tipoMedia());
        }

        Disciplina atualizada = disciplinaRepository.save(disciplina);
        return toResponseDTO(atualizada);
    }

    public Double calcularMedia(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina);

        return calcularMedia(disciplina, avaliacoes);
    }

    private Double calcularMedia(Disciplina disciplina, List<Avaliacao> avaliacoes) {

        if (disciplina.getTipoMedia() == null) {
            return null;
        }

        avaliacoes = avaliacoes.stream().filter(Avaliacao::getConcluida).toList();

        if (avaliacoes.isEmpty()) {
            return null;
        }

        if (disciplina.getTipoMedia() == TipoMediaEnum.SIMPLES) {
            return avaliacoes.stream()
                    .mapToDouble(Avaliacao::getNotaObtida)
                    .average()
                    .orElse(0.0);
        }

        double somaNotasPonderadas = avaliacoes.stream()
                .mapToDouble(a -> a.getNotaObtida() * a.getPeso())
                .sum();

        double somaPesos = avaliacoes.stream()
                .mapToDouble(Avaliacao::getPeso)
                .sum();

        return somaPesos == 0 ? 0.0 : somaNotasPonderadas / somaPesos;
    }

    public MediaResponseDTO buscarMedia(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina);
        Double media = calcularMedia(disciplina, avaliacoes);

        Double faltaParaAprovacao = null;

        if (media != null) {
            faltaParaAprovacao = Math.max(0.0, disciplina.getUsuario().getMediaAprovacao() - media);
        }

        List<AvaliacaoMediaResponseDTO> avaliacoesDTO = avaliacoes.stream().map(this::toAvaliacaoMediaResponseDTO).toList();

        return new MediaResponseDTO(
                disciplina.getTipoMedia(),
                media,
                disciplina.getUsuario().getMediaAprovacao(),
                faltaParaAprovacao,
                avaliacoesDTO
        );
    }

    public DisciplinaResponseDTO atualizarStatusDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        Double media = calcularMedia(idDisciplina, idUsuarioLogado);

        if (media == null) {
            disciplina.setStatus(StatusDisciplinaEnum.EM_PROGRESSO);
        } else if (media >= MEDIA_MINIMA_APROVACAO) {
            disciplina.setStatus(StatusDisciplinaEnum.APROVADO);
        } else {
            disciplina.setStatus(StatusDisciplinaEnum.REPROVADO);
        }

        Disciplina atualizada = disciplinaRepository.save(disciplina);
        return toResponseDTO(atualizada);
    }

    public void arquivarDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        disciplina.setArquivada(true);
        disciplina.setStatus(StatusDisciplinaEnum.ARQUIVADA);
        disciplinaRepository.save(disciplina);
    }

    public void deletarDisciplina(UUID id, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        boolean temAvaliacoes = !avaliacaoRepository.findByDisciplinaIdDisciplina(id).isEmpty();
        boolean temTarefas = !tarefaRepository.findByDisciplinaIdDisciplina(id).isEmpty();

        if (temAvaliacoes || temTarefas) {
            throw new DisciplinaComDadosVinculadosException(
                "Não é possível excluir uma disciplina com avaliações ou tarefas vinculadas. Arquive-a em vez disso.");
        }

        disciplinaRepository.delete(disciplina);
    }

    private Disciplina buscarEntidadeDoUsuario(UUID id, UUID idUsuarioLogado) {
        return disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(id, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada"));
    }

    private AvaliacaoMediaResponseDTO toAvaliacaoMediaResponseDTO(Avaliacao avaliacao) {
        return new AvaliacaoMediaResponseDTO(
                avaliacao.getIdAvaliacao(),
                avaliacao.getNomeAvaliacao(),
                avaliacao.getNotaObtida(),
                avaliacao.getNotaMaxima(),
                avaliacao.getPeso(),
                avaliacao.getConcluida()
        );
    }

    private DisciplinaResponseDTO toResponseDTO(Disciplina disciplina) {
        List<HorarioResponseDTO> horariosDTO = horarioRepository.findByDisciplinaIdDisciplina(disciplina.getIdDisciplina())
                .stream()
                .map(h -> new HorarioResponseDTO(h.getIdHora(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFim()))
                .toList();

        Double media = calcularMedia(disciplina.getIdDisciplina(), disciplina.getUsuario().getIdUsuario());
        Double faltaParaAprovacao = Math.max(0.0, disciplina.getUsuario().getMediaAprovacao() - media);

        return new DisciplinaResponseDTO(
                disciplina.getIdDisciplina(),
                disciplina.getNomeDisciplina(),
                disciplina.getPeriodo(),
                disciplina.getProfessor(),
                disciplina.getSala(),
                disciplina.getAndar(),
                disciplina.getCor(),
                disciplina.getStatus(),
                disciplina.getTipoMedia(),
                media,
                faltaParaAprovacao,
                disciplina.getArquivada(),
                horariosDTO
        );
    }
}