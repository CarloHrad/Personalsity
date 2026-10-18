package com.example.trinots.DisciplinaServiceTest;

import com.example.trinots.domain.Avaliacao;
import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.Tarefa;
import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import com.example.trinots.domain.enums.TipoMediaEnum;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaRequestDTO;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaResponseDTO;
import com.example.trinots.exception.exceptions.*;
import com.example.trinots.repository.AvaliacaoRepository;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.repository.HorarioRepository;
import com.example.trinots.repository.TarefaRepository;
import com.example.trinots.service.DisciplinaService;
import com.example.trinots.service.HorarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisciplinaService")
class DisciplinaServiceTest {

    private DisciplinaRepository disciplinaRepository;
    private HorarioRepository horarioRepository;
    private AvaliacaoRepository avaliacaoRepository;
    private TarefaRepository tarefaRepository;
    private DisciplinaService disciplinaService;
    private HorarioService horarioService;

    private UUID idUsuarioLogado;
    private Usuario usuarioLogado;
    private UUID idDisciplina;
    private Disciplina disciplinaExistente;

    @BeforeEach
    void setUp() {
        disciplinaRepository = mock(DisciplinaRepository.class);
        horarioRepository = mock(HorarioRepository.class);
        avaliacaoRepository = mock(AvaliacaoRepository.class);
        tarefaRepository = mock(TarefaRepository.class);
        horarioService = mock(HorarioService.class);
        disciplinaService = new DisciplinaService(disciplinaRepository, avaliacaoRepository, horarioRepository, tarefaRepository, horarioService);

        idUsuarioLogado = UUID.randomUUID();
        usuarioLogado = new Usuario();
        usuarioLogado.setIdUsuario(idUsuarioLogado);

        idDisciplina = UUID.randomUUID();
        disciplinaExistente = new Disciplina();
        disciplinaExistente.setIdDisciplina(idDisciplina);
        disciplinaExistente.setNomeDisciplina("Cálculo 1");
        disciplinaExistente.setPeriodo(2);
        disciplinaExistente.setStatus(StatusDisciplinaEnum.EM_PROGRESSO);
        disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
        disciplinaExistente.setArquivada(false);
        disciplinaExistente.setUsuario(usuarioLogado);

        lenient().when(horarioRepository.findByDisciplinaIdDisciplina(any())).thenReturn(List.of());
    }

    private DisciplinaRequestDTO criarDtoValido(TipoMediaEnum tipoMedia) {
        return new DisciplinaRequestDTO("Cálculo 1", 2, "Prof. Ana", "Sala 3", 1, "#FF5733", tipoMedia, null);
    }

    private Avaliacao criarAvaliacao(Double nota, Double peso, boolean concluida) {
        return criarAvaliacaoComMaxima(nota, 10.0, peso, concluida);
    }

    private Avaliacao criarAvaliacaoComMaxima(Double nota, Double notaMaxima, Double peso, boolean concluida) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setIdAvaliacao(UUID.randomUUID());
        avaliacao.setNotaMaxima(notaMaxima);
        avaliacao.setNotaObtida(nota);
        avaliacao.setPeso(peso);
        avaliacao.setConcluida(concluida);
        return avaliacao;
    }

    @Nested
    @DisplayName("criarDisciplina")
    class CriarDisciplina {

        /*@Test
        @DisplayName("deve criar disciplina com sucesso quando nome não existe para o usuário")
        void deveCriarComSucesso() {
            DisciplinaRequestDTO dto = criarDtoValido(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuario(dto.nomeDisciplina(), idUsuarioLogado))
                    .thenReturn(false);
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.criarDisciplina(dto, usuarioLogado);

            assertThat(resultado.nomeDisciplina()).isEqualTo("Cálculo 1");
            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.EM_PROGRESSO);
            assertThat(resultado.arquivada()).isFalse();
        }

        @Test
        @DisplayName("deve lançar DisciplinaJaCadastradaException quando nome já existe para o mesmo usuário")
        void deveLancarExceptionQuandoNomeDuplicado() {
            DisciplinaRequestDTO dto = criarDtoValido(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuario(dto.nomeDisciplina(), idUsuarioLogado))
                    .thenReturn(true);

            assertThatThrownBy(() -> disciplinaService.criarDisciplina(dto, usuarioLogado))
                    .isInstanceOf(DisciplinaJaCadastradaException.class);

            verify(disciplinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve permitir criar disciplina sem TipoMedia definido (professor ainda não decidiu)")
        void devePermitirTipoMediaNulo() {
            DisciplinaRequestDTO dto = criarDtoValido(null);
            when(disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuario(any(), any())).thenReturn(false);
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.criarDisciplina(dto, usuarioLogado);

            assertThat(resultado.tipoMedia()).isNull();
        }*/

        /*@Test
        @DisplayName("sempre deve definir status como EM_PROGRESSO na criação, mesmo que não venha no DTO")
        void deveSempreDefinirStatusEmProgresso() {
            DisciplinaRequestDTO dto = criarDtoValido(TipoMediaEnum.PONDERADA);
            when(disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuario(any(), any())).thenReturn(false);
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.criarDisciplina(dto, usuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.EM_PROGRESSO);
        }*/
    }

    @Nested
    @DisplayName("buscarDisciplinaPorId — IDOR")
    class BuscarDisciplinaPorId {

        /*@Test
        @DisplayName("deve retornar disciplina quando pertence ao usuário logado")
        void deveRetornarQuandoPertenceAoUsuario() {
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            DisciplinaResponseDTO resultado = disciplinaService.buscarDisciplinaPorId(idDisciplina, idUsuarioLogado);

            assertThat(resultado.idDisciplina()).isEqualTo(idDisciplina);
        }*/

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando disciplina pertence a OUTRO usuário (IDOR)")
        void deveLancarExceptionQuandoPertenceAOutroUsuario() {
            UUID idOutroUsuario = UUID.randomUUID();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> disciplinaService.buscarDisciplinaPorId(idDisciplina, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando id não existe")
        void deveLancarExceptionQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idInexistente, idUsuarioLogado))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> disciplinaService.buscarDisciplinaPorId(idInexistente, idUsuarioLogado))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarDisciplinasAtivas")
    class ListarDisciplinasAtivas {

        /*@Test
        @DisplayName("deve retornar apenas disciplinas não arquivadas do usuário")
        void deveRetornarApenasNaoArquivadas() {
            Disciplina arquivada = new Disciplina();
            arquivada.setIdDisciplina(UUID.randomUUID());
            arquivada.setArquivada(true);
            arquivada.setUsuario(usuarioLogado);

            when(disciplinaRepository.findByUsuarioIdUsuario(idUsuarioLogado)).thenReturn(List.of(disciplinaExistente, arquivada));

            List<DisciplinaResponseDTO> resultado = disciplinaService.listarDisciplinasAtivas(idUsuarioLogado);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).idDisciplina()).isEqualTo(idDisciplina);
        }*/

        @Test
        @DisplayName("deve retornar lista vazia quando usuário não tem disciplinas")
        void deveRetornarListaVaziaQuandoSemDisciplinas() {
            when(disciplinaRepository.findByUsuarioIdUsuario(idUsuarioLogado)).thenReturn(List.of());

            List<DisciplinaResponseDTO> resultado = disciplinaService.listarDisciplinasAtivas(idUsuarioLogado);

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("atualizarDisciplina")
    class AtualizarDisciplina {

        /*@Test
        @DisplayName("deve atualizar disciplina com sucesso quando não está arquivada")
        void deveAtualizarComSucesso() {
            DisciplinaRequestDTO dto = new DisciplinaRequestDTO("Cálculo 2", 3, "Prof. Bruno", "Sala 5", 2, "#00FF00", TipoMediaEnum.PONDERADA, null);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarDisciplina(idDisciplina, dto, idUsuarioLogado);

            assertThat(resultado.nomeDisciplina()).isEqualTo("Cálculo 2");
            assertThat(resultado.tipoMedia()).isEqualTo(TipoMediaEnum.PONDERADA);
        }*/

        @Test
        @DisplayName("deve lançar DisciplinaArquivadaException ao tentar editar disciplina arquivada")
        void deveLancarExceptionQuandoArquivada() {
            disciplinaExistente.setArquivada(true);
            DisciplinaRequestDTO dto = criarDtoValido(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            assertThatThrownBy(() -> disciplinaService.atualizarDisciplina(idDisciplina, dto, idUsuarioLogado))
                    .isInstanceOf(DisciplinaArquivadaException.class);

            verify(disciplinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve permitir renomear para um nome que o usuário já usa em outra disciplina")
        void naoDevePermitirRenomearParaNomeJaExistente() {
            DisciplinaRequestDTO dto = criarDtoValido(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(disciplinaRepository.existsByNomeDisciplinaAndUsuarioIdUsuarioAndIdDisciplinaNot(
                    dto.nomeDisciplina(), idUsuarioLogado, idDisciplina)).thenReturn(true);

            assertThatThrownBy(() -> disciplinaService.atualizarDisciplina(idDisciplina, dto, idUsuarioLogado))
                    .isInstanceOf(DisciplinaJaCadastradaException.class);

            verify(disciplinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve permitir atualizar disciplina de outro usuário")
        void naoDevePermitirAtualizarDeOutroUsuario() {
            DisciplinaRequestDTO dto = criarDtoValido(TipoMediaEnum.SIMPLES);
            UUID idOutroUsuario = UUID.randomUUID();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> disciplinaService.atualizarDisciplina(idDisciplina, dto, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(disciplinaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("calcularMedia")
    class CalcularMedia {

        @Test
        @DisplayName("deve retornar null quando TipoMedia não está definido")
        void deveRetornarNullQuandoTipoMediaNulo() {
            disciplinaExistente.setTipoMedia(null);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            Double media = disciplinaService.calcularMedia(idDisciplina, idUsuarioLogado);

            assertThat(media).isNull();
            verify(avaliacaoRepository).findByDisciplinaIdDisciplina(any());
        }

        @Test
        @DisplayName("deve retornar null quando não há avaliações concluídas")
        void deveRetornarNullQuandoSemAvaliacoesConcluidas() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(criarAvaliacao(8.0, 1.0, false)));

            Double media = disciplinaService.calcularMedia(idDisciplina, idUsuarioLogado);

            assertThat(media).isNull();
        }

        @Test
        @DisplayName("deve calcular média SIMPLES corretamente (aritmética)")
        void deveCalcularMediaSimples() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(
                    criarAvaliacao(8.0, 1.0, true),
                    criarAvaliacao(6.0, 1.0, true),
                    criarAvaliacao(10.0, 1.0, true)
            ));

            Double media = disciplinaService.calcularMedia(idDisciplina, idUsuarioLogado);

            assertThat(media).isEqualTo(8.0);
        }

        @Test
        @DisplayName("deve calcular média PONDERADA corretamente com pesos livres (não normalizados)")
        void deveCalcularMediaPonderada() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.PONDERADA);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(
                    criarAvaliacao(8.0, 3.0, true),
                    criarAvaliacao(4.0, 1.0, true)
            ));

            Double media = disciplinaService.calcularMedia(idDisciplina, idUsuarioLogado);

            assertThat(media).isEqualTo(7.0);
        }

        @Test
        @DisplayName("deve ignorar avaliações não concluídas no cálculo da média")
        void deveIgnorarAvaliacoesNaoConcluidas() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(
                    criarAvaliacao(10.0, 1.0, true),
                    criarAvaliacao(0.0, 1.0, false)
            ));

            Double media = disciplinaService.calcularMedia(idDisciplina, idUsuarioLogado);

            assertThat(media).isEqualTo(10.0);
        }

        @Test
        @DisplayName("não deve lançar ArithmeticException quando soma dos pesos é zero")
        void naoDeveLancarExceptionQuandoSomaPesosZero() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.PONDERADA);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(
                    criarAvaliacao(8.0, 0.0, true)
            ));

            Double media = disciplinaService.calcularMedia(idDisciplina, idUsuarioLogado);

            assertThat(media).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("atualizarStatusDisciplina")
    class AtualizarStatusDisciplina {

        @Test
        @DisplayName("deve definir status como APROVADO quando média >= 6.0")
        void deveAprovarQuandoMediaSuficiente() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(criarAvaliacao(7.0, 1.0, true)));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.APROVADO);
        }

        @Test
        @DisplayName("deve definir status como REPROVADO quando média < 6.0")
        void deveReprovarQuandoMediaInsuficiente() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(criarAvaliacao(5.9, 1.0, true)));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.REPROVADO);
        }

        @Test
        @DisplayName("deve considerar exatamente 6.0 como APROVADO (limite inclusivo)")
        void deveAprovarComMediaExatamenteNoLimite() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(criarAvaliacao(6.0, 1.0, true)));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.APROVADO);
        }

        @Test
        @DisplayName("deve manter EM_PROGRESSO enquanto houver avaliação pendente, mesmo com média parcial baixa")
        void deveManterEmProgressoComAvaliacaoPendente() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(
                    criarAvaliacao(4.0, 1.0, true),
                    criarAvaliacao(null, 1.0, false)));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.EM_PROGRESSO);
            assertThat(resultado.mediaAtual()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("deve fechar o status pela média quando todas as avaliações cadastradas estão concluídas")
        void deveFecharStatusQuandoTodasConcluidas() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(
                    criarAvaliacao(4.0, 1.0, true),
                    criarAvaliacao(5.0, 1.0, true)));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.REPROVADO);
        }

        @Test
        @DisplayName("deve normalizar a nota pela nota máxima: 35 de 50 vira 7.0 e aprova")
        void deveNormalizarNotaPelaNotaMaxima() {
            disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(criarAvaliacaoComMaxima(35.0, 50.0, 1.0, true)));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.mediaAtual()).isEqualTo(7.0);
            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.APROVADO);
        }

        @Test
        @DisplayName("não deve recalcular nem salvar disciplina arquivada: o status ARQUIVADA é preservado")
        void naoDeveAlterarDisciplinaArquivada() {
            disciplinaExistente.setArquivada(true);
            disciplinaExistente.setStatus(StatusDisciplinaEnum.ARQUIVADA);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(criarAvaliacao(9.0, 1.0, true)));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.ARQUIVADA);
            assertThat(disciplinaExistente.getStatus()).isEqualTo(StatusDisciplinaEnum.ARQUIVADA);
            verify(disciplinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("recalcular o status de todas as disciplinas deve ignorar as arquivadas")
        void recalcularTodasDeveIgnorarArquivadas() {
            Disciplina arquivada = new Disciplina();
            arquivada.setIdDisciplina(UUID.randomUUID());
            arquivada.setArquivada(true);
            arquivada.setStatus(StatusDisciplinaEnum.ARQUIVADA);
            arquivada.setUsuario(usuarioLogado);

            when(disciplinaRepository.findByUsuarioIdUsuario(idUsuarioLogado)).thenReturn(List.of(arquivada, disciplinaExistente));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of());
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            disciplinaService.recalcularStatusDeTodasAsDisciplinas(idUsuarioLogado);

            assertThat(arquivada.getStatus()).isEqualTo(StatusDisciplinaEnum.ARQUIVADA);
            verify(disciplinaRepository, never()).findByIdDisciplinaAndUsuarioIdUsuario(eq(arquivada.getIdDisciplina()), any());
        }

        /*@Test
        @DisplayName("deve manter status EM_PROGRESSO quando ainda não há média calculável")
        void deveManterEmProgressoQuandoSemMedia() {
            disciplinaExistente.setTipoMedia(null);
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.EM_PROGRESSO);
        }
    }*/

        @Nested
        @DisplayName("projeção e risco de reprovação")
        class ProjecaoERisco {

            private void prepararDisciplinaComAvaliacoes(Avaliacao... avaliacoes) {
                disciplinaExistente.setTipoMedia(TipoMediaEnum.SIMPLES);
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(avaliacoes));
            }

            @Test
            @DisplayName("pendente fica fora da média atual e entra como zero só na projeção pessimista")
            void pendenteSoEntraComoZeroNaProjecao() {
                prepararDisciplinaComAvaliacoes(criarAvaliacao(8.0, 1.0, true), criarAvaliacao(null, 1.0, false));

                var media = disciplinaService.buscarMedia(idDisciplina, idUsuarioLogado);

                assertThat(media.mediaAtual()).isEqualTo(8.0);
                assertThat(media.projecaoPessimista()).isEqualTo(4.0);
                assertThat(media.notaNecessariaRestantes()).isEqualTo(4.0);
                assertThat(media.emRiscoReprovacao()).isFalse();
            }

            @Test
            @DisplayName("deve marcar risco quando a nota necessária nas restantes passa da nota máxima possível (10)")
            void deveMarcarRiscoQuandoNotaNecessariaMaiorQueDez() {
                prepararDisciplinaComAvaliacoes(
                        criarAvaliacao(1.0, 1.0, true), criarAvaliacao(0.0, 1.0, true), criarAvaliacao(null, 1.0, false));

                var media = disciplinaService.buscarMedia(idDisciplina, idUsuarioLogado);

                assertThat(media.notaNecessariaRestantes()).isEqualTo(17.0);
                assertThat(media.emRiscoReprovacao()).isTrue();
            }

            @Test
            @DisplayName("não deve marcar risco quando a nota necessária é exatamente 10")
            void naoDeveMarcarRiscoNoLimiteExato() {
                prepararDisciplinaComAvaliacoes(criarAvaliacao(2.0, 1.0, true), criarAvaliacao(null, 1.0, false));
                disciplinaExistente.getUsuario().setMediaAprovacao(6.0);

                var media = disciplinaService.buscarMedia(idDisciplina, idUsuarioLogado);

                assertThat(media.notaNecessariaRestantes()).isEqualTo(10.0);
                assertThat(media.emRiscoReprovacao()).isFalse();
            }

            @Test
            @DisplayName("deve expor a nota normalizada (0–10) de cada avaliação")
            void deveExporNotaNormalizada() {
                prepararDisciplinaComAvaliacoes(criarAvaliacaoComMaxima(35.0, 50.0, 1.0, true));

                var media = disciplinaService.buscarMedia(idDisciplina, idUsuarioLogado);

                assertThat(media.avaliacoes()).hasSize(1);
                assertThat(media.avaliacoes().get(0).notaNormalizada()).isEqualTo(7.0);
            }

            @Test
            @DisplayName("na lista de disciplinas, arquivada nunca aparece em risco de reprovação")
            void arquivadaNaoApareceEmRisco() {
                prepararDisciplinaComAvaliacoes(
                        criarAvaliacao(0.0, 1.0, true), criarAvaliacao(0.0, 1.0, true), criarAvaliacao(null, 1.0, false));
                disciplinaExistente.setArquivada(true);
                disciplinaExistente.setStatus(StatusDisciplinaEnum.ARQUIVADA);

                DisciplinaResponseDTO resultado = disciplinaService.atualizarStatusDisciplina(idDisciplina, idUsuarioLogado);

                assertThat(resultado.emRiscoReprovacao()).isFalse();
            }
        }

        @Nested
        @DisplayName("desarquivarDisciplina")
        class DesarquivarDisciplina {

            @BeforeEach
            void arquivar() {
                disciplinaExistente.setArquivada(true);
                disciplinaExistente.setStatus(StatusDisciplinaEnum.ARQUIVADA);
            }

            @Test
            @DisplayName("deve desarquivar e voltar o status a ser derivado da média")
            void deveDesarquivarERecalcularStatus() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                        .thenReturn(List.of(criarAvaliacao(7.0, 1.0, true)));
                when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

                DisciplinaResponseDTO resultado = disciplinaService.desarquivarDisciplina(idDisciplina, idUsuarioLogado);

                assertThat(resultado.arquivada()).isFalse();
                assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.APROVADO);
                verify(horarioService).validarHorariosDaDisciplina(disciplinaExistente, idUsuarioLogado);
            }

            @Test
            @DisplayName("deve voltar para EM_PROGRESSO quando não há avaliações")
            void deveVoltarParaEmProgressoSemAvaliacoes() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of());
                when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(inv -> inv.getArgument(0));

                DisciplinaResponseDTO resultado = disciplinaService.desarquivarDisciplina(idDisciplina, idUsuarioLogado);

                assertThat(resultado.status()).isEqualTo(StatusDisciplinaEnum.EM_PROGRESSO);
            }

            @Test
            @DisplayName("não deve desarquivar quando os horários passam a conflitar com outra disciplina ativa")
            void naoDeveDesarquivarComConflitoDeHorario() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                doThrow(new HorarioConflitanteException("conflito"))
                        .when(horarioService).validarHorariosDaDisciplina(any(Disciplina.class), any(UUID.class));

                assertThatThrownBy(() -> disciplinaService.desarquivarDisciplina(idDisciplina, idUsuarioLogado))
                        .isInstanceOf(HorarioConflitanteException.class);

                assertThat(disciplinaExistente.getArquivada()).isTrue();
                verify(disciplinaRepository, never()).save(any());
            }

            @Test
            @DisplayName("deve lançar DisciplinaNaoArquivadaException quando a disciplina não está arquivada")
            void deveLancarQuandoNaoArquivada() {
                disciplinaExistente.setArquivada(false);
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));

                assertThatThrownBy(() -> disciplinaService.desarquivarDisciplina(idDisciplina, idUsuarioLogado))
                        .isInstanceOf(DisciplinaNaoArquivadaException.class);

                verify(disciplinaRepository, never()).save(any());
            }

            @Test
            @DisplayName("não deve permitir desarquivar disciplina de outro usuário")
            void naoDevePermitirDesarquivarDeOutroUsuario() {
                UUID idOutroUsuario = UUID.randomUUID();
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idOutroUsuario))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> disciplinaService.desarquivarDisciplina(idDisciplina, idOutroUsuario))
                        .isInstanceOf(EntityNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("arquivarDisciplina")
        class ArquivarDisciplina {

            @Test
            @DisplayName("deve arquivar disciplina e definir status como ARQUIVADA")
            void deveArquivarComSucesso() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));

                disciplinaService.arquivarDisciplina(idDisciplina, idUsuarioLogado);

                assertThat(disciplinaExistente.getArquivada()).isTrue();
                assertThat(disciplinaExistente.getStatus()).isEqualTo(StatusDisciplinaEnum.ARQUIVADA);
                verify(disciplinaRepository).save(disciplinaExistente);
            }

            @Test
            @DisplayName("não deve permitir arquivar disciplina de outro usuário")
            void naoDevePermitirArquivarDeOutroUsuario() {
                UUID idOutroUsuario = UUID.randomUUID();
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idOutroUsuario))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> disciplinaService.arquivarDisciplina(idDisciplina, idOutroUsuario))
                        .isInstanceOf(EntityNotFoundException.class);

                verify(disciplinaRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("deletarDisciplina")
        class DeletarDisciplina {

            @Test
            @DisplayName("deve deletar com sucesso quando não há avaliações nem tarefas vinculadas")
            void deveDeletarComSucesso() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of());
                when(tarefaRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of());

                disciplinaService.deletarDisciplina(idDisciplina, idUsuarioLogado);

                verify(disciplinaRepository).delete(disciplinaExistente);
            }

            @Test
            @DisplayName("deve lançar DisciplinaComDadosVinculadosException quando há avaliações vinculadas")
            void deveLancarExceptionQuandoHaAvaliacoes() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina))
                        .thenReturn(List.of(criarAvaliacao(8.0, 1.0, true)));
                when(tarefaRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of());

                assertThatThrownBy(() -> disciplinaService.deletarDisciplina(idDisciplina, idUsuarioLogado))
                        .isInstanceOf(DisciplinaComDadosVinculadosException.class);

                verify(disciplinaRepository, never()).delete(any());
            }

            @Test
            @DisplayName("deve lançar DisciplinaComDadosVinculadosException quando há tarefas vinculadas")
            void deveLancarExceptionQuandoHaTarefas() {
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(avaliacaoRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of());
                when(tarefaRepository.findByDisciplinaIdDisciplina(idDisciplina)).thenReturn(List.of(new Tarefa()));

                assertThatThrownBy(() -> disciplinaService.deletarDisciplina(idDisciplina, idUsuarioLogado))
                        .isInstanceOf(DisciplinaComDadosVinculadosException.class);

                verify(disciplinaRepository, never()).delete(any());
            }

            @Test
            @DisplayName("não deve permitir deletar disciplina de outro usuário")
            void naoDevePermitirDeletarDeOutroUsuario() {
                UUID idOutroUsuario = UUID.randomUUID();
                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idOutroUsuario))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> disciplinaService.deletarDisciplina(idDisciplina, idOutroUsuario))
                        .isInstanceOf(EntityNotFoundException.class);
            }
        }
    }
}