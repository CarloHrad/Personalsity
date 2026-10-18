package com.example.trinots.service;

import com.example.trinots.domain.Avaliacao;
import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.Tarefa;
import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.StatusDisciplinaEnum;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DisciplinaService {

    private final DisciplinaRepository disciplinaRepository;
    private final HorarioRepository horarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final TarefaRepository tarefaRepository;
    private final HorarioService horarioService;

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
        List<Disciplina> disciplinas = disciplinaRepository.findByUsuarioIdUsuario(idUsuarioLogado)
                .stream()
                .filter(d -> !d.getArquivada())
                .toList();
        return toResponseDTOList(disciplinas);
    }

    public List<DisciplinaResponseDTO> listarDisciplinas(Usuario usuario, StatusDisciplinaEnum status) {
        Integer periodoAtual = usuario.getSemestreAtual();

        List<Disciplina> disciplinas = (status == null)
                ? disciplinaRepository.findByUsuarioIdUsuarioAndPeriodo(usuario.getIdUsuario(), periodoAtual)
                : disciplinaRepository.findByUsuarioIdUsuarioAndPeriodoAndStatus(usuario.getIdUsuario(), periodoAtual, status);

        return toResponseDTOList(disciplinas);
    }

    public List<DisciplinaResponseDTO> listarArquivadas(Integer periodo, UUID idUsuarioLogado) {
        List<Disciplina> disciplinas = (periodo != null)
                ? disciplinaRepository.findByUsuarioIdUsuarioAndPeriodoAndArquivadaTrue(idUsuarioLogado, periodo)
                : disciplinaRepository.findByUsuarioIdUsuarioAndArquivadaTrue(idUsuarioLogado);

        return toResponseDTOList(disciplinas);
    }

    public DisciplinaResponseDTO atualizarDisciplina(UUID id, DisciplinaRequestDTO dto, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível editar uma disciplina arquivada");
        }

        if (disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuarioAndIdDisciplinaNot(dto.nomeDisciplina(), idUsuarioLogado, id)) {
            throw new DisciplinaJaCadastradaException("Você já tem uma disciplina chamada '" + dto.nomeDisciplina() + "'");
        }

        boolean temAvaliacoes = !avaliacaoRepository.findByDisciplinaIdDisciplina(id).isEmpty();

        boolean tentandoAlterarTipoMedia = dto.tipoMedia() != null
                && !Objects.equals(disciplina.getTipoMedia(), dto.tipoMedia());

        if (temAvaliacoes && tentandoAlterarTipoMedia) {
            throw new TipoMediaImutavelException(
                    "Não é possível alterar o tipo de média: a disciplina já possui avaliações cadastradas");
        }

        disciplina.setNomeDisciplina(dto.nomeDisciplina());
        disciplina.setPeriodo(dto.periodo());
        disciplina.setProfessor(dto.professor());
        disciplina.setSala(dto.sala());
        disciplina.setAndar(dto.andar());
        disciplina.setCor(dto.cor());

        if (dto.tipoMedia() != null && !temAvaliacoes) {
            disciplina.setTipoMedia(dto.tipoMedia());
        }

        Disciplina atualizada = disciplinaRepository.save(disciplina);
        return toResponseDTO(atualizada);
    }

    public Double calcularMedia(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina);
        return calcularResultado(disciplina, avaliacoes).mediaAtual();
    }

    /**
     * Calcula média atual, projeção e risco. Notas são normalizadas para 0–10 (nota / notaMaxima * 10);
     * avaliações pendentes ficam fora da média atual e só entram (como zero) na projeção pessimista.
     */
    private MediaCalculator.Resultado calcularResultado(Disciplina disciplina, List<Avaliacao> avaliacoes) {
        List<MediaCalculator.Item> itens = avaliacoes.stream()
                .map(a -> new MediaCalculator.Item(
                        a.getNotaObtida(), a.getNotaMaxima(), a.getPeso(), Boolean.TRUE.equals(a.getConcluida())))
                .toList();

        return MediaCalculator.calcular(disciplina.getTipoMedia(), itens, disciplina.getUsuario().getMediaAprovacao());
    }

    /**
     * O status é fechado pela média somente quando todas as avaliações cadastradas já foram concluídas.
     * Enquanto houver avaliação pendente, a disciplina segue EM_PROGRESSO.
     * RECUPERACAO ainda não é atribuída automaticamente (limite de recuperação não definido).
     */
    private StatusDisciplinaEnum definirStatus(Disciplina disciplina, MediaCalculator.Resultado resultado) {
        if (resultado.mediaAtual() == null || !resultado.todasConcluidas()) {
            return StatusDisciplinaEnum.EM_PROGRESSO;
        }
        return resultado.mediaAtual() >= disciplina.getUsuario().getMediaAprovacao()
                ? StatusDisciplinaEnum.APROVADO
                : StatusDisciplinaEnum.REPROVADO;
    }

    public MediaResponseDTO buscarMedia(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina);
        MediaCalculator.Resultado resultado = calcularResultado(disciplina, avaliacoes);
        Double media = resultado.mediaAtual();

        Double faltaParaAprovacao = (media != null)
                ? Math.max(0.0, disciplina.getUsuario().getMediaAprovacao() - media)
                : null;

        List<AvaliacaoMediaResponseDTO> avaliacoesDTO = avaliacoes.stream().map(this::toAvaliacaoMediaResponseDTO).toList();

        return new MediaResponseDTO(
                disciplina.getTipoMedia(),
                media,
                disciplina.getUsuario().getMediaAprovacao(),
                faltaParaAprovacao,
                avaliacoesDTO,
                resultado.projecaoPessimista(),
                resultado.notaNecessariaRestantes(),
                resultado.emRiscoReprovacao()
        );
    }

    public DisciplinaResponseDTO atualizarStatusDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina);

        if (Boolean.TRUE.equals(disciplina.getArquivada())) {
            return toResponseDTO(disciplina, avaliacoes);
        }

        disciplina.setStatus(definirStatus(disciplina, calcularResultado(disciplina, avaliacoes)));

        Disciplina atualizada = disciplinaRepository.save(disciplina);
        return toResponseDTO(atualizada, avaliacoes);
    }

    public void arquivarDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);
        disciplina.setArquivada(true);
        disciplina.setStatus(StatusDisciplinaEnum.ARQUIVADA);
        disciplinaRepository.save(disciplina);
    }

    /** Desfaz o arquivamento: o status volta a ser derivado da média e os horários são revalidados. */
    public DisciplinaResponseDTO desarquivarDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarEntidadeDoUsuario(idDisciplina, idUsuarioLogado);

        if (!Boolean.TRUE.equals(disciplina.getArquivada())) {
            throw new DisciplinaNaoArquivadaException("A disciplina não está arquivada");
        }

        horarioService.validarHorariosDaDisciplina(disciplina, idUsuarioLogado);

        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina);
        disciplina.setArquivada(false);
        disciplina.setStatus(definirStatus(disciplina, calcularResultado(disciplina, avaliacoes)));

        Disciplina atualizada = disciplinaRepository.save(disciplina);
        return toResponseDTO(atualizada, avaliacoes);
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

    public void recalcularStatusDeTodasAsDisciplinas(UUID idUsuarioLogado) {
        List<Disciplina> disciplinas = disciplinaRepository.findByUsuarioIdUsuario(idUsuarioLogado);
        for (Disciplina disciplina : disciplinas) {
            if (Boolean.TRUE.equals(disciplina.getArquivada())) {
                continue;
            }
            atualizarStatusDisciplina(disciplina.getIdDisciplina(), idUsuarioLogado);
        }
    }

    private AvaliacaoMediaResponseDTO toAvaliacaoMediaResponseDTO(Avaliacao avaliacao) {
        return new AvaliacaoMediaResponseDTO(
                avaliacao.getIdAvaliacao(),
                avaliacao.getNomeAvaliacao(),
                avaliacao.getNotaObtida(),
                avaliacao.getNotaMaxima(),
                avaliacao.getPeso(),
                avaliacao.getConcluida(),
                MediaCalculator.notaNormalizada(new MediaCalculator.Item(
                        avaliacao.getNotaObtida(), avaliacao.getNotaMaxima(), avaliacao.getPeso(),
                        Boolean.TRUE.equals(avaliacao.getConcluida())))
        );
    }

    private List<DisciplinaResponseDTO> toResponseDTOList(List<Disciplina> disciplinas) {
        if (disciplinas.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = disciplinas.stream().map(Disciplina::getIdDisciplina).toList();

        Map<UUID, List<Avaliacao>> avaliacoesPorDisciplina = avaliacaoRepository
                .findByDisciplinaIdDisciplinaIn(ids)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getDisciplina().getIdDisciplina()));

        Map<UUID, List<HorarioResponseDTO>> horariosPorDisciplina = horarioRepository
                .findByDisciplinaIdDisciplinaIn(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        h -> h.getDisciplina().getIdDisciplina(),
                        Collectors.mapping(
                                h -> new HorarioResponseDTO(h.getIdHora(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFim()),
                                Collectors.toList()
                        )
                ));

        return disciplinas.stream()
                .map(d -> toResponseDTO(
                        d,
                        avaliacoesPorDisciplina.getOrDefault(d.getIdDisciplina(), List.of()),
                        horariosPorDisciplina.getOrDefault(d.getIdDisciplina(), List.of())
                ))
                .toList();
    }

    private DisciplinaResponseDTO toResponseDTO(Disciplina disciplina, List<Avaliacao> avaliacoes, List<HorarioResponseDTO> horariosDTO) {
        MediaCalculator.Resultado resultado = calcularResultado(disciplina, avaliacoes);
        Double media = resultado.mediaAtual();
        Double faltaParaAprovacao = (media != null)
                ? Math.max(0.0, disciplina.getUsuario().getMediaAprovacao() - media)
                : null;

        boolean arquivada = Boolean.TRUE.equals(disciplina.getArquivada());

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
                horariosDTO,
                resultado.projecaoPessimista(),
                resultado.notaNecessariaRestantes(),
                !arquivada && resultado.emRiscoReprovacao()
        );
    }

    private DisciplinaResponseDTO toResponseDTO(Disciplina disciplina, List<Avaliacao> avaliacoes) {
        List<HorarioResponseDTO> horariosDTO = horarioRepository.findByDisciplinaIdDisciplina(disciplina.getIdDisciplina())
                .stream()
                .map(h -> new HorarioResponseDTO(h.getIdHora(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFim()))
                .toList();

        return toResponseDTO(disciplina, avaliacoes, horariosDTO);
    }

    private DisciplinaResponseDTO toResponseDTO(Disciplina disciplina) {
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDisciplinaIdDisciplina(disciplina.getIdDisciplina());
        return toResponseDTO(disciplina, avaliacoes);
    }
}