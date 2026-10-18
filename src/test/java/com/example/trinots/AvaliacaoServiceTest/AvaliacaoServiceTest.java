package com.example.trinots.AvaliacaoServiceTest;

import com.example.trinots.domain.Avaliacao;
import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoConcluirDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoRequestDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoResponseDTO;
import com.example.trinots.exception.exceptions.*;
import com.example.trinots.repository.AvaliacaoRepository;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.service.AvaliacaoService;
import com.example.trinots.service.DisciplinaService;
import com.example.trinots.service.FusoHorarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvaliacaoService")
class AvaliacaoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 12, 15, 0);

    private AvaliacaoRepository avaliacaoRepository;
    private DisciplinaRepository disciplinaRepository;
    private DisciplinaService disciplinaService;
    private FusoHorarioService fusoHorarioService;
    private AvaliacaoService avaliacaoService;

    private UUID idUsuario;
    private UUID idDisciplina;
    private UUID idAvaliacao;
    private Disciplina disciplina;
    private Avaliacao avaliacao;

    @BeforeEach
    void setUp() {
        avaliacaoRepository = mock(AvaliacaoRepository.class);
        disciplinaRepository = mock(DisciplinaRepository.class);
        disciplinaService = mock(DisciplinaService.class);
        fusoHorarioService = mock(FusoHorarioService.class);
        lenient().when(fusoHorarioService.agora()).thenReturn(AGORA);
        avaliacaoService = new AvaliacaoService(avaliacaoRepository, disciplinaRepository, disciplinaService, fusoHorarioService);

        idUsuario = UUID.randomUUID();
        idDisciplina = UUID.randomUUID();
        idAvaliacao = UUID.randomUUID();

        disciplina = new Disciplina();
        disciplina.setIdDisciplina(idDisciplina);
        disciplina.setNomeDisciplina("Cálculo 1");
        disciplina.setTipoMedia(TipoMediaEnum.SIMPLES);
        disciplina.setArquivada(false);

        avaliacao = new Avaliacao();
        avaliacao.setIdAvaliacao(idAvaliacao);
        avaliacao.setNomeAvaliacao("P1");
        avaliacao.setDataAvaliacao(LocalDate.of(2026, 10, 20));
        avaliacao.setNotaMaxima(10.0);
        avaliacao.setPeso(1.0);
        avaliacao.setConcluida(false);
        avaliacao.setDisciplina(disciplina);
    }

    private void disciplinaDoUsuario() {
        when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuario))
                .thenReturn(Optional.of(disciplina));
    }

    private void avaliacaoDoUsuario() {
        when(avaliacaoRepository.findByIdAvaliacaoAndDisciplinaUsuarioIdUsuario(idAvaliacao, idUsuario))
                .thenReturn(Optional.of(avaliacao));
    }

    private void salvarRetornaArgumento() {
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void concluirAvaliacaoExistente(double nota) {
        avaliacao.setConcluida(true);
        avaliacao.setNotaObtida(nota);
        avaliacao.setDataConclusao(AGORA.minusDays(1));
    }

    private AvaliacaoRequestDTO dto(String nome, Double nota, Double max, Double peso) {
        return new AvaliacaoRequestDTO(nome, null, LocalDate.of(2026, 10, 20), nota, max, peso, idDisciplina);
    }

    @Nested
    @DisplayName("criarAvaliacao")
    class CriarAvaliacao {

        @Test
        @DisplayName("sem nota, cria pendente e recalcula o status (nova pendente reabre disciplina fechada)")
        void criaPendenteERecalculaStatus() {
            disciplinaDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.criarAvaliacao(dto("P1", null, 10.0, null), idUsuario);

            assertThat(resultado.concluida()).isFalse();
            assertThat(resultado.notaObtida()).isNull();
            assertThat(resultado.dataConclusao()).isNull();
            verify(disciplinaService).atualizarStatusDisciplina(idDisciplina, idUsuario);
        }

        @Test
        @DisplayName("com nota, cria já concluída com a data de conclusão no fuso do usuário")
        void criaConcluidaComFuso() {
            disciplinaDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.criarAvaliacao(dto("P1", 8.0, 10.0, null), idUsuario);

            assertThat(resultado.concluida()).isTrue();
            assertThat(resultado.dataConclusao()).isEqualTo(AGORA);
            verify(disciplinaService).atualizarStatusDisciplina(idDisciplina, idUsuario);
        }

        @Test
        @DisplayName("média simples força peso 1.0, ignorando o peso informado")
        void mediaSimplesForcaPesoUm() {
            disciplinaDoUsuario();
            ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
            when(avaliacaoRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            avaliacaoService.criarAvaliacao(dto("P1", null, 10.0, 5.0), idUsuario);

            assertThat(captor.getValue().getPeso()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("média ponderada exige peso")
        void ponderadaExigePeso() {
            disciplina.setTipoMedia(TipoMediaEnum.PONDERADA);
            disciplinaDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.criarAvaliacao(dto("P1", null, 10.0, null), idUsuario))
                    .isInstanceOf(PesoObrigatorioException.class);

            verify(avaliacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("nota obtida maior que a nota máxima é rejeitada")
        void notaMaiorQueMaxima() {
            disciplinaDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.criarAvaliacao(dto("P1", 11.0, 10.0, null), idUsuario))
                    .isInstanceOf(NotaInvalidaException.class);
        }

        @Test
        @DisplayName("nota máxima diferente de 10 é aceita (a normalização acontece no cálculo da média)")
        void aceitaNotaMaximaDiferenteDeDez() {
            disciplinaDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.criarAvaliacao(dto("Prova", 35.0, 50.0, null), idUsuario);

            assertThat(resultado.notaObtida()).isEqualTo(35.0);
            assertThat(resultado.notaMaxima()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("sem tipo de média definido, não cria avaliação")
        void semTipoDeMedia() {
            disciplina.setTipoMedia(null);
            disciplinaDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.criarAvaliacao(dto("P1", null, 10.0, null), idUsuario))
                    .isInstanceOf(TipoMediaIndefinidaException.class);
        }

        @Test
        @DisplayName("disciplina arquivada não aceita avaliação")
        void disciplinaArquivada() {
            disciplina.setArquivada(true);
            disciplinaDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.criarAvaliacao(dto("P1", null, 10.0, null), idUsuario))
                    .isInstanceOf(DisciplinaArquivadaException.class);
        }

        @Test
        @DisplayName("disciplina de outro usuário não é encontrada")
        void disciplinaDeOutroUsuario() {
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> avaliacaoService.criarAvaliacao(dto("P1", null, 10.0, null), idUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(avaliacaoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("concluirAvaliacao")
    class ConcluirAvaliacao {

        @Test
        @DisplayName("registra a nota, marca como concluída e recalcula o status")
        void deveConcluir() {
            avaliacaoDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.concluirAvaliacao(idAvaliacao, new AvaliacaoConcluirDTO(7.5), idUsuario);

            assertThat(resultado.concluida()).isTrue();
            assertThat(resultado.notaObtida()).isEqualTo(7.5);
            assertThat(resultado.dataConclusao()).isEqualTo(AGORA);
            verify(disciplinaService).atualizarStatusDisciplina(idDisciplina, idUsuario);
        }

        @Test
        @DisplayName("nota acima da nota máxima é rejeitada")
        void notaAcimaDaMaxima() {
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.concluirAvaliacao(idAvaliacao, new AvaliacaoConcluirDTO(10.5), idUsuario))
                    .isInstanceOf(NotaInvalidaException.class);

            assertThat(avaliacao.getConcluida()).isFalse();
        }

        @Test
        @DisplayName("avaliação já concluída não pode ser concluída de novo")
        void jaConcluida() {
            concluirAvaliacaoExistente(8.0);
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.concluirAvaliacao(idAvaliacao, new AvaliacaoConcluirDTO(9.0), idUsuario))
                    .isInstanceOf(AvaliacaoJaConcluidaException.class);
        }

        @Test
        @DisplayName("disciplina arquivada não permite concluir")
        void disciplinaArquivada() {
            disciplina.setArquivada(true);
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.concluirAvaliacao(idAvaliacao, new AvaliacaoConcluirDTO(9.0), idUsuario))
                    .isInstanceOf(DisciplinaArquivadaException.class);
        }

        @Test
        @DisplayName("avaliação de outro usuário não é encontrada")
        void deOutroUsuario() {
            when(avaliacaoRepository.findByIdAvaliacaoAndDisciplinaUsuarioIdUsuario(idAvaliacao, idUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> avaliacaoService.concluirAvaliacao(idAvaliacao, new AvaliacaoConcluirDTO(9.0), idUsuario))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reabrirAvaliacao")
    class ReabrirAvaliacao {

        @Test
        @DisplayName("limpa a nota e a data de conclusão e recalcula o status")
        void deveReabrir() {
            concluirAvaliacaoExistente(8.0);
            avaliacaoDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.reabrirAvaliacao(idAvaliacao, idUsuario);

            assertThat(resultado.concluida()).isFalse();
            assertThat(resultado.notaObtida()).isNull();
            assertThat(resultado.dataConclusao()).isNull();
            verify(disciplinaService).atualizarStatusDisciplina(idDisciplina, idUsuario);
        }

        @Test
        @DisplayName("avaliação pendente não pode ser reaberta")
        void naoConcluida() {
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.reabrirAvaliacao(idAvaliacao, idUsuario))
                    .isInstanceOf(AvaliacaoNaoConcluidaException.class);
        }
    }

    @Nested
    @DisplayName("atualizarAvaliacao")
    class AtualizarAvaliacao {

        @Test
        @DisplayName("avaliação concluída em média simples pode ser renomeada sem reenviar peso nem nota")
        void renomearConcluidaEmMediaSimples() {
            concluirAvaliacaoExistente(8.0);
            avaliacaoDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.atualizarAvaliacao(
                    idAvaliacao, dto("Prova 1 (renomeada)", null, 10.0, null), idUsuario);

            assertThat(resultado.nomeAvaliacao()).isEqualTo("Prova 1 (renomeada)");
            assertThat(resultado.notaObtida()).isEqualTo(8.0);
            assertThat(resultado.peso()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("avaliação concluída não permite mudar a nota máxima")
        void concluidaNaoMudaNotaMaxima() {
            concluirAvaliacaoExistente(8.0);
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.atualizarAvaliacao(idAvaliacao, dto("P1", null, 20.0, null), idUsuario))
                    .isInstanceOf(AvaliacaoJaConcluidaException.class);

            verify(avaliacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("avaliação concluída não permite mudar a nota por este endpoint")
        void concluidaNaoMudaNota() {
            concluirAvaliacaoExistente(8.0);
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.atualizarAvaliacao(idAvaliacao, dto("P1", 9.0, 10.0, null), idUsuario))
                    .isInstanceOf(AvaliacaoJaConcluidaException.class);
        }

        @Test
        @DisplayName("média ponderada: avaliação concluída não permite mudar o peso, mas aceita o mesmo peso")
        void concluidaPonderadaPeso() {
            disciplina.setTipoMedia(TipoMediaEnum.PONDERADA);
            avaliacao.setPeso(3.0);
            concluirAvaliacaoExistente(8.0);
            avaliacaoDoUsuario();
            salvarRetornaArgumento();

            assertThatThrownBy(() -> avaliacaoService.atualizarAvaliacao(idAvaliacao, dto("P1", null, 10.0, 4.0), idUsuario))
                    .isInstanceOf(AvaliacaoJaConcluidaException.class);

            assertThat(avaliacaoService.atualizarAvaliacao(idAvaliacao, dto("P1 revisada", null, 10.0, 3.0), idUsuario)
                    .nomeAvaliacao()).isEqualTo("P1 revisada");
        }

        @Test
        @DisplayName("avaliação pendente: nota não pode ser definida por aqui")
        void pendenteNaoRecebeNota() {
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.atualizarAvaliacao(idAvaliacao, dto("P1", 8.0, 10.0, null), idUsuario))
                    .isInstanceOf(NotaInvalidaException.class);
        }

        @Test
        @DisplayName("avaliação pendente: atualiza nota máxima e peso")
        void pendenteAtualizaCampos() {
            disciplina.setTipoMedia(TipoMediaEnum.PONDERADA);
            avaliacaoDoUsuario();
            salvarRetornaArgumento();

            AvaliacaoResponseDTO resultado = avaliacaoService.atualizarAvaliacao(
                    idAvaliacao, dto("P1", null, 50.0, 4.0), idUsuario);

            assertThat(resultado.notaMaxima()).isEqualTo(50.0);
            assertThat(resultado.peso()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("não permite mover a avaliação para outra disciplina")
        void naoMoveDeDisciplina() {
            avaliacaoDoUsuario();
            AvaliacaoRequestDTO paraOutra = new AvaliacaoRequestDTO("P1", null, LocalDate.of(2026, 10, 20),
                    null, 10.0, null, UUID.randomUUID());

            assertThatThrownBy(() -> avaliacaoService.atualizarAvaliacao(idAvaliacao, paraOutra, idUsuario))
                    .isInstanceOf(DisciplinaImutavelException.class);
        }

        @Test
        @DisplayName("disciplina arquivada não permite editar")
        void disciplinaArquivada() {
            disciplina.setArquivada(true);
            avaliacaoDoUsuario();

            assertThatThrownBy(() -> avaliacaoService.atualizarAvaliacao(idAvaliacao, dto("P1", null, 10.0, null), idUsuario))
                    .isInstanceOf(DisciplinaArquivadaException.class);
        }
    }

    @Nested
    @DisplayName("deletarAvaliacao")
    class DeletarAvaliacao {

        @Test
        @DisplayName("remover avaliação pendente também recalcula o status (as restantes podem estar todas concluídas)")
        void removerPendenteRecalculaStatus() {
            avaliacaoDoUsuario();

            avaliacaoService.deletarAvaliacao(idAvaliacao, idUsuario);

            verify(avaliacaoRepository).delete(avaliacao);
            verify(disciplinaService).atualizarStatusDisciplina(idDisciplina, idUsuario);
        }

        @Test
        @DisplayName("permitido mesmo com a disciplina arquivada")
        void permitidoComDisciplinaArquivada() {
            disciplina.setArquivada(true);
            avaliacaoDoUsuario();

            avaliacaoService.deletarAvaliacao(idAvaliacao, idUsuario);

            verify(avaliacaoRepository).delete(avaliacao);
        }

        @Test
        @DisplayName("avaliação de outro usuário não é encontrada")
        void deOutroUsuario() {
            when(avaliacaoRepository.findByIdAvaliacaoAndDisciplinaUsuarioIdUsuario(idAvaliacao, idUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> avaliacaoService.deletarAvaliacao(idAvaliacao, idUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(avaliacaoRepository, never()).delete(any(Avaliacao.class));
        }
    }

    @Nested
    @DisplayName("buscarAvaliacaoPorId / listarPorDisciplina")
    class Consultas {

        @Test
        @DisplayName("busca por id respeita o isolamento entre usuários")
        void buscaPorIdIsolada() {
            when(avaliacaoRepository.findByIdAvaliacaoAndDisciplinaUsuarioIdUsuario(idAvaliacao, idUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> avaliacaoService.buscarAvaliacaoPorId(idAvaliacao, idUsuario))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("listagem por disciplina de outro usuário não é permitida")
        void listagemIsolada() {
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> avaliacaoService.listarPorDisciplina(idDisciplina, idUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(avaliacaoRepository, never()).findByDisciplinaIdDisciplina(any());
        }
    }
}
