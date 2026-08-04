package com.example.trinots.service;

import com.example.trinots.domain.Avaliacao;
import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoConcluirDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoRequestDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoResponseDTO;
import com.example.trinots.exception.PesoObrigatorioException;
import com.example.trinots.exception.exceptions.*;
import com.example.trinots.repository.AvaliacaoRepository;
import com.example.trinots.repository.DisciplinaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final DisciplinaService disciplinaService;


    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                            DisciplinaRepository disciplinaRepository,
                            DisciplinaService disciplinaService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.disciplinaService = disciplinaService;
    }


    public AvaliacaoResponseDTO criarAvaliacao(AvaliacaoRequestDTO dto, UUID idUsuarioLogado) {
        Disciplina disciplina = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível adicionar avaliação a uma disciplina arquivada");
        }

        if (disciplina.getTipoMedia() == null) {
            throw new TipoMediaIndefinidaException("Defina o tipo de média da disciplina antes de cadastrar avaliações");
        }

        if (dto.notaObtida() != null && dto.notaObtida() > dto.notaMaxima()) {
            throw new NotaInvalidaException(
                    "Nota obtida não pode ser maior que a nota máxima (" + dto.notaMaxima() + ")");
        }

        Double peso = resolverPeso(disciplina, dto.peso());
        boolean jaConcluida = dto.notaObtida() != null;

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNomeAvaliacao(dto.nomeAvaliacao());
        avaliacao.setDescricao(dto.descricao());
        avaliacao.setDataAvaliacao(dto.dataAvaliacao());
        avaliacao.setNotaMaxima(dto.notaMaxima());
        avaliacao.setPeso(peso);
        avaliacao.setNotaObtida(dto.notaObtida());
        avaliacao.setConcluida(jaConcluida);
        avaliacao.setDataConclusao(jaConcluida ? LocalDateTime.now() : null);
        avaliacao.setDisciplina(disciplina);

        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        if (jaConcluida) {
            disciplinaService.atualizarStatusDisciplina(disciplina.getIdDisciplina(), idUsuarioLogado);
        }

        return toResponseDTO(salva);
    }


    public AvaliacaoResponseDTO buscarAvaliacaoPorId(UUID id, UUID idUsuarioLogado) {
        return toResponseDTO(buscarEntidadeDoUsuario(id, idUsuarioLogado));
    }


    public List<AvaliacaoResponseDTO> listarPorDisciplina(UUID idDisciplina, UUID idUsuarioLogado) {
        buscarDisciplinaDoUsuario(idDisciplina, idUsuarioLogado);
        return avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }


    public AvaliacaoResponseDTO atualizarAvaliacao(UUID id, AvaliacaoRequestDTO dto, UUID idUsuarioLogado) {
        Avaliacao avaliacao = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        Disciplina disciplina = buscarDisciplinaDoUsuario(dto.idDisciplina(), idUsuarioLogado);

        if (disciplina.getArquivada()) {
            throw new DisciplinaArquivadaException("Não é possível editar avaliação de uma disciplina arquivada");
        }

        if (avaliacao.getConcluida()) {
            boolean tentandoAlterarCampoTravado =
                    !avaliacao.getNotaMaxima().equals(dto.notaMaxima()) ||
                            !Objects.equals(avaliacao.getPeso(), dto.peso()) ||
                            !Objects.equals(avaliacao.getNotaObtida(), dto.notaObtida()) ||
                            !avaliacao.getDisciplina().getIdDisciplina().equals(dto.idDisciplina());

            if (tentandoAlterarCampoTravado) {
                throw new AvaliacaoJaConcluidaException(
                        "Avaliação já concluída: só é possível editar nome e descrição. Para alterar a nota, use reabrir + concluir novamente.");
            }

            avaliacao.setNomeAvaliacao(dto.nomeAvaliacao());
            avaliacao.setDescricao(dto.descricao());
            avaliacao.setDataAvaliacao(dto.dataAvaliacao());

            return toResponseDTO(avaliacaoRepository.save(avaliacao));
        }

        if (dto.notaObtida() != null) {
            throw new NotaInvalidaException(
                    "Não é possível definir a nota por aqui. Use o endpoint de concluir avaliação.");
        }

        Double peso = resolverPeso(disciplina, dto.peso());

        avaliacao.setNomeAvaliacao(dto.nomeAvaliacao());
        avaliacao.setDescricao(dto.descricao());
        avaliacao.setDataAvaliacao(dto.dataAvaliacao());
        avaliacao.setNotaMaxima(dto.notaMaxima());
        avaliacao.setPeso(peso);
        avaliacao.setDisciplina(disciplina);

        return toResponseDTO(avaliacaoRepository.save(avaliacao));
    }


    public AvaliacaoResponseDTO concluirAvaliacao(UUID id, AvaliacaoConcluirDTO dto, UUID idUsuarioLogado) {
        Avaliacao avaliacao = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (avaliacao.getConcluida()) {
            throw new AvaliacaoJaConcluidaException("Avaliação já está concluída");
        }

        if (dto.notaObtida() > avaliacao.getNotaMaxima()) {
            throw new NotaInvalidaException(
                    "Nota obtida não pode ser maior que a nota máxima (" + avaliacao.getNotaMaxima() + ")");
        }

        avaliacao.setNotaObtida(dto.notaObtida());
        avaliacao.setConcluida(true);
        avaliacao.setDataConclusao(LocalDateTime.now());

        Avaliacao atualizada = avaliacaoRepository.save(avaliacao);

        disciplinaService.atualizarStatusDisciplina(avaliacao.getDisciplina().getIdDisciplina(), idUsuarioLogado);

        return toResponseDTO(atualizada);
    }


    public AvaliacaoResponseDTO reabrirAvaliacao(UUID id, UUID idUsuarioLogado) {
        Avaliacao avaliacao = buscarEntidadeDoUsuario(id, idUsuarioLogado);

        if (!avaliacao.getConcluida()) {
            throw new AvaliacaoNaoConcluidaException("Avaliação ainda não foi concluída");
        }

        avaliacao.setConcluida(false);
        avaliacao.setNotaObtida(null);
        avaliacao.setDataConclusao(null);

        Avaliacao atualizada = avaliacaoRepository.save(avaliacao);

        disciplinaService.atualizarStatusDisciplina(avaliacao.getDisciplina().getIdDisciplina(), idUsuarioLogado);

        return toResponseDTO(atualizada);
    }


    public void deletarAvaliacao(UUID id, UUID idUsuarioLogado) {
        Avaliacao avaliacao = buscarEntidadeDoUsuario(id, idUsuarioLogado);
        UUID idDisciplina = avaliacao.getDisciplina().getIdDisciplina();
        boolean estavaConcluida = avaliacao.getConcluida();

        avaliacaoRepository.delete(avaliacao);

        if (estavaConcluida) {
            disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);
        }
    }


    private Double resolverPeso(Disciplina disciplina, Double pesoInformado) {
        if (disciplina.getTipoMedia() == TipoMediaEnum.PONDERADA && pesoInformado == null) {
            throw new PesoObrigatorioException("Peso é obrigatório para disciplinas com média ponderada");
        }
        return disciplina.getTipoMedia() == TipoMediaEnum.SIMPLES ? 1.0 : pesoInformado;
    }


    private Disciplina buscarDisciplinaDoUsuario(UUID idDisciplina, UUID idUsuarioLogado) {
        return disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada"));
    }


    private Avaliacao buscarEntidadeDoUsuario(UUID id, UUID idUsuarioLogado) {
        return avaliacaoRepository.findByIdAvaliacaoAndDisciplinaUsuarioIdUsuario(id, idUsuarioLogado)
                .orElseThrow(() -> new EntityNotFoundException("Avaliação não encontrada"));
    }


    private AvaliacaoResponseDTO toResponseDTO(Avaliacao avaliacao) {
        return new AvaliacaoResponseDTO(
                avaliacao.getIdAvaliacao(),
                avaliacao.getNomeAvaliacao(),
                avaliacao.getDescricao(),
                avaliacao.getDataAvaliacao(),
                avaliacao.getDataConclusao(),
                avaliacao.getNotaObtida(),
                avaliacao.getNotaMaxima(),
                avaliacao.getPeso(),
                avaliacao.getConcluida(),
                avaliacao.getDisciplina().getNomeDisciplina()
        );
    }
}