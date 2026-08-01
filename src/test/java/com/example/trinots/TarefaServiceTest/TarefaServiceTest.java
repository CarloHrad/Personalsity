package com.example.trinots.TarefaServiceTest;

import com.example.trinots.domain.*;
import com.example.trinots.domain.enums.TipoTarefaEnum;
import com.example.trinots.dto.TarefaDTO.TarefaRequestDTO;
import com.example.trinots.dto.TarefaDTO.TarefaResponseDTO;
import com.example.trinots.exception.exceptions.*;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.repository.TarefaRepository;
import com.example.trinots.service.TarefaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TarefaService")
class TarefaServiceTest {

    private TarefaRepository tarefaRepository;
    private DisciplinaRepository disciplinaRepository;
    private TarefaService tarefaService;

    private UUID idUsuarioLogado;
    private UUID idDisciplina;
    private Disciplina disciplinaExistente;
    private UUID idTarefa;
    private Tarefa tarefaExistente;

    @BeforeEach
    void setUp() {
        tarefaRepository = mock(TarefaRepository.class);
        disciplinaRepository = mock(DisciplinaRepository.class);
        tarefaService = new TarefaService(tarefaRepository, disciplinaRepository);

        idUsuarioLogado = UUID.randomUUID();
        idDisciplina = UUID.randomUUID();

        disciplinaExistente = new Disciplina();
        disciplinaExistente.setIdDisciplina(idDisciplina);
        disciplinaExistente.setNomeDisciplina("Cálculo 1");
        disciplinaExistente.setArquivada(false);

        idTarefa = UUID.randomUUID();
        tarefaExistente = new Tarefa();
        tarefaExistente.setIdTarefa(idTarefa);
        tarefaExistente.setNomeTarefa("Lista 1");
        tarefaExistente.setTipoTarefa(TipoTarefaEnum.LISTA_EXERCICIOS);
        tarefaExistente.setDataEntrega(LocalDate.now().plusDays(5));
        tarefaExistente.setConcluida(false);
        tarefaExistente.setDataConclusao(null);
        tarefaExistente.setDisciplina(disciplinaExistente);
    }

    private TarefaRequestDTO criarDtoValido() {
        return new TarefaRequestDTO("Lista 1", "Exercícios do capítulo 3",
                TipoTarefaEnum.LISTA_EXERCICIOS, LocalDate.now().plusDays(5), idDisciplina);
    }

    @Nested
    @DisplayName("criarTarefa")
    class CriarTarefa {

        @Test
        @DisplayName("deve criar tarefa com sucesso quando disciplina pertence ao usuário e não está arquivada")
        void deveCriarComSucesso() {
            TarefaRequestDTO dto = criarDtoValido();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

            TarefaResponseDTO resultado = tarefaService.criarTarefa(dto, idUsuarioLogado);

            assertThat(resultado.nomeTarefa()).isEqualTo("Lista 1");
            assertThat(resultado.concluida()).isFalse();
            assertThat(resultado.dataConclusao()).isNull();
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando disciplina não pertence ao usuário (IDOR)")
        void deveLancarExceptionQuandoDisciplinaNaoEhDoUsuario() {
            TarefaRequestDTO dto = criarDtoValido();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.criarTarefa(dto, idUsuarioLogado))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(tarefaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar DisciplinaArquivadaException quando disciplina está arquivada")
        void deveLancarExceptionQuandoDisciplinaArquivada() {
            disciplinaExistente.setArquivada(true);
            TarefaRequestDTO dto = criarDtoValido();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            assertThatThrownBy(() -> tarefaService.criarTarefa(dto, idUsuarioLogado))
                    .isInstanceOf(DisciplinaArquivadaException.class);

            verify(tarefaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve criar tarefa sempre com concluida=false e dataConclusao=null, mesmo que o DTO tentasse influenciar isso")
        void deveSempreCriarComoNaoConcluida() {
            TarefaRequestDTO dto = criarDtoValido();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

            TarefaResponseDTO resultado = tarefaService.criarTarefa(dto, idUsuarioLogado);

            assertThat(resultado.concluida()).isFalse();
            assertThat(resultado.dataConclusao()).isNull();
        }
    }

    @Nested
    @DisplayName("buscarTarefaPorId — IDOR")
    class BuscarTarefaPorId {

        @Test
        @DisplayName("deve retornar tarefa quando pertence ao usuário")
        void deveRetornarQuandoPertenceAoUsuario() {
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));

            TarefaResponseDTO resultado = tarefaService.buscarTarefaPorId(idTarefa, idUsuarioLogado);

            assertThat(resultado.idTarefa()).isEqualTo(idTarefa);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando tarefa pertence a outro usuário")
        void deveLancarExceptionQuandoPertenceAOutroUsuario() {
            UUID idOutroUsuario = UUID.randomUUID();
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.buscarTarefaPorId(idTarefa, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarPorDisciplina")
    class ListarPorDisciplina {

        @Test
        @DisplayName("deve listar tarefas quando disciplina pertence ao usuário")
        void deveListarComSucesso() {
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(tarefaRepository.findByDisciplinaIdDisciplina(idDisciplina))
                    .thenReturn(List.of(tarefaExistente));

            List<TarefaResponseDTO> resultado = tarefaService.listarPorDisciplina(idDisciplina, idUsuarioLogado);

            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando disciplina não pertence ao usuário (IDOR)")
        void deveLancarExceptionQuandoDisciplinaNaoEhDoUsuario() {
            UUID idOutroUsuario = UUID.randomUUID();
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.listarPorDisciplina(idDisciplina, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(tarefaRepository, never()).findByDisciplinaIdDisciplina(any());
        }
    }

    @Nested
    @DisplayName("listarPendentes")
    class ListarPendentes {

        @Test
        @DisplayName("deve listar apenas tarefas pendentes do usuário logado")
        void deveListarApenasDoUsuarioLogado() {
            when(tarefaRepository.findByConcluidaFalseAndDisciplinaUsuarioIdUsuario(idUsuarioLogado))
                    .thenReturn(List.of(tarefaExistente));

            List<TarefaResponseDTO> resultado = tarefaService.listarPendentes(idUsuarioLogado);

            assertThat(resultado).hasSize(1);
            verify(tarefaRepository).findByConcluidaFalseAndDisciplinaUsuarioIdUsuario(idUsuarioLogado);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há tarefas pendentes")
        void deveRetornarListaVaziaQuandoSemPendentes() {
            when(tarefaRepository.findByConcluidaFalseAndDisciplinaUsuarioIdUsuario(idUsuarioLogado))
                    .thenReturn(List.of());

            List<TarefaResponseDTO> resultado = tarefaService.listarPendentes(idUsuarioLogado);

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("atualizarTarefa")
    class AtualizarTarefa {

        @Test
        @DisplayName("deve atualizar tarefa com sucesso quando não está concluída nem a disciplina arquivada")
        void deveAtualizarComSucesso() {
            TarefaRequestDTO dto = new TarefaRequestDTO("Lista 2", "Nova descrição",
                    TipoTarefaEnum.ESTUDO, LocalDate.now().plusDays(10), idDisciplina);

            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

            TarefaResponseDTO resultado = tarefaService.atualizarTarefa(idTarefa, dto, idUsuarioLogado);

            assertThat(resultado.nomeTarefa()).isEqualTo("Lista 2");
            assertThat(resultado.tipoTarefa()).isEqualTo(TipoTarefaEnum.ESTUDO);
        }

        @Test
        @DisplayName("deve lançar TarefaJaConcluidaException ao tentar editar tarefa já concluída")
        void deveLancarExceptionQuandoJaConcluida() {
            tarefaExistente.setConcluida(true);
            TarefaRequestDTO dto = criarDtoValido();

            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            assertThatThrownBy(() -> tarefaService.atualizarTarefa(idTarefa, dto, idUsuarioLogado))
                    .isInstanceOf(TarefaJaConcluidaException.class);

            verify(tarefaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar DisciplinaArquivadaException ao tentar editar tarefa de disciplina arquivada")
        void deveLancarExceptionQuandoDisciplinaArquivada() {
            disciplinaExistente.setArquivada(true);
            TarefaRequestDTO dto = criarDtoValido();

            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            assertThatThrownBy(() -> tarefaService.atualizarTarefa(idTarefa, dto, idUsuarioLogado))
                    .isInstanceOf(DisciplinaArquivadaException.class);

            verify(tarefaRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve permitir atualizar tarefa de outro usuário (IDOR)")
        void naoDevePermitirAtualizarDeOutroUsuario() {
            UUID idOutroUsuario = UUID.randomUUID();
            TarefaRequestDTO dto = criarDtoValido();
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.atualizarTarefa(idTarefa, dto, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(tarefaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("concluirTarefa")
    class ConcluirTarefa {

        @Test
        @DisplayName("deve concluir tarefa com sucesso, preenchendo dataConclusao com dataAvaliacao e hora atuais")
        void deveConcluirComSucesso() {
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime antes = LocalDateTime.now();
            TarefaResponseDTO resultado = tarefaService.concluirTarefa(idTarefa, idUsuarioLogado);
            LocalDateTime depois = LocalDateTime.now();

            assertThat(resultado.concluida()).isTrue();
            assertThat(resultado.dataConclusao()).isNotNull();
            assertThat(resultado.dataConclusao()).isBetween(antes, depois);
        }

        @Test
        @DisplayName("deve lançar TarefaJaConcluidaException ao tentar concluir tarefa já concluída")
        void deveLancarExceptionQuandoJaConcluida() {
            tarefaExistente.setConcluida(true);
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));

            assertThatThrownBy(() -> tarefaService.concluirTarefa(idTarefa, idUsuarioLogado))
                    .isInstanceOf(TarefaJaConcluidaException.class);

            verify(tarefaRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve permitir concluir tarefa de outro usuário (IDOR)")
        void naoDevePermitirConcluirDeOutroUsuario() {
            UUID idOutroUsuario = UUID.randomUUID();
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.concluirTarefa(idTarefa, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(tarefaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reabrirTarefa")
    class ReabrirTarefa {

        @Test
        @DisplayName("deve reabrir tarefa concluída, limpando concluida e dataConclusao")
        void deveReabrirComSucesso() {
            tarefaExistente.setConcluida(true);
            tarefaExistente.setDataConclusao(LocalDateTime.now());

            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));
            when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));

            TarefaResponseDTO resultado = tarefaService.reabrirTarefa(idTarefa, idUsuarioLogado);

            assertThat(resultado.concluida()).isFalse();
            assertThat(resultado.dataConclusao()).isNull();
        }

        @Test
        @DisplayName("deve lançar TarefaNaoConcluidaException ao tentar reabrir tarefa que ainda não foi concluída")
        void deveLancarExceptionQuandoNaoConcluida() {
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente)); // concluida = false por padrão do setUp

            assertThatThrownBy(() -> tarefaService.reabrirTarefa(idTarefa, idUsuarioLogado))
                    .isInstanceOf(TarefaNaoConcluidaException.class);

            verify(tarefaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deletarTarefa")
    class DeletarTarefa {

        @Test
        @DisplayName("deve deletar tarefa com sucesso quando pertence ao usuário")
        void deveDeletarComSucesso() {
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idUsuarioLogado))
                    .thenReturn(Optional.of(tarefaExistente));

            tarefaService.deletarTarefa(idTarefa, idUsuarioLogado);

            verify(tarefaRepository).delete(tarefaExistente);
        }

        @Test
        @DisplayName("não deve permitir deletar tarefa de outro usuário (IDOR)")
        void naoDevePermitirDeletarDeOutroUsuario() {
            UUID idOutroUsuario = UUID.randomUUID();
            when(tarefaRepository.findByIdTarefaAndDisciplinaUsuarioIdUsuario(idTarefa, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tarefaService.deletarTarefa(idTarefa, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(tarefaRepository, never()).delete(any());
        }
    }
}