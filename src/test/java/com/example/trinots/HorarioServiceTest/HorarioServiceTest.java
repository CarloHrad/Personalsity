package com.example.trinots.HorarioServiceTest;

import com.example.trinots.domain.Disciplina;
import com.example.trinots.domain.Horario;
import com.example.trinots.domain.enums.DiaSemanaEnum;
import com.example.trinots.dto.HorarioDTO.HorarioRequestDTO;
import com.example.trinots.dto.HorarioDTO.HorarioResponseDTO;
import com.example.trinots.exception.exceptions.DisciplinaArquivadaException;
import com.example.trinots.exception.exceptions.HorarioConflitanteException;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.repository.HorarioRepository;
import com.example.trinots.service.HorarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HorarioService")
class HorarioServiceTest {

    private HorarioRepository horarioRepository;
    private DisciplinaRepository disciplinaRepository;
    private HorarioService horarioService;

    private UUID idUsuarioLogado;
    private UUID idDisciplina;
    private Disciplina disciplinaExistente;

    @BeforeEach
    void setUp() {
        horarioRepository = mock(HorarioRepository.class);
        disciplinaRepository = mock(DisciplinaRepository.class);
        horarioService = new HorarioService(horarioRepository, disciplinaRepository);

        idUsuarioLogado = UUID.randomUUID();
        idDisciplina = UUID.randomUUID();

        disciplinaExistente = new Disciplina();
        disciplinaExistente.setIdDisciplina(idDisciplina);
        disciplinaExistente.setNomeDisciplina("Cálculo 1");
        disciplinaExistente.setArquivada(false);
    }

    private HorarioRequestDTO criarDto(DiaSemanaEnum dia, LocalTime inicio, LocalTime fim) {
        return new HorarioRequestDTO(dia, inicio, fim, idDisciplina);
    }

    private Horario criarHorarioExistente(UUID id, DiaSemanaEnum dia, LocalTime inicio, LocalTime fim) {
        Horario horario = new Horario();
        horario.setIdHora(id);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(inicio);
        horario.setHoraFim(fim);
        horario.setDisciplina(disciplinaExistente);
        return horario;
    }

    @Nested
    @DisplayName("criarHorario")
    class CriarHorario {

        @Test
        @DisplayName("deve criar horário com sucesso quando não há conflito")
        void deveCriarComSucesso() {
            HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                    .thenReturn(List.of());
            when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

            HorarioResponseDTO resultado = horarioService.criarHorario(dto, idUsuarioLogado);

            assertThat(resultado.horaInicio()).isEqualTo(LocalTime.of(8, 0));
            assertThat(resultado.horaFim()).isEqualTo(LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando disciplina não existe ou não é do usuário")
        void deveLancarExceptionQuandoDisciplinaNaoEncontrada() {
            HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(horarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar DisciplinaArquivadaException quando disciplina está arquivada")
        void deveLancarExceptionQuandoDisciplinaArquivada() {
            disciplinaExistente.setArquivada(true);
            HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));

            assertThatThrownBy(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                    .isInstanceOf(DisciplinaArquivadaException.class);

            verify(horarioRepository, never()).save(any());
        }

        @Nested
        @DisplayName("cenários de sobreposição")
        class Sobreposicao {

            @ParameterizedTest(name = "existente=[{0}-{1}] novo=[{2}-{3}] deve conflitar")
            @CsvSource({
                    "08:00, 10:00, 09:00, 11:00",
                    "08:00, 10:00, 07:00, 09:00",
                    "08:00, 10:00, 08:30, 09:30",
                    "08:00, 10:00, 07:00, 11:00",
                    "08:00, 10:00, 08:00, 10:00",
            })
            @DisplayName("deve lançar HorarioConflitanteException em qualquer tipo de sobreposição")
            void deveLancarExceptionEmSobreposicao(String inicioExistente, String fimExistente,
                                                   String inicioNovo, String fimNovo) {
                Horario existente = criarHorarioExistente(UUID.randomUUID(), DiaSemanaEnum.SEGUNDA,
                        LocalTime.parse(inicioExistente), LocalTime.parse(fimExistente));

                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA,
                        LocalTime.parse(inicioNovo), LocalTime.parse(fimNovo));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                        .thenReturn(List.of(existente));

                assertThatThrownBy(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .isInstanceOf(HorarioConflitanteException.class);

                verify(horarioRepository, never()).save(any());
            }

            @ParameterizedTest(name = "existente=[{0}-{1}] novo=[{2}-{3}] NÃO deve conflitar")
            @CsvSource({
                    "08:00, 10:00, 10:00, 12:00",
                    "08:00, 10:00, 06:00, 08:00",
                    "08:00, 10:00, 11:00, 12:00",
            })
            @DisplayName("não deve conflitar quando horários são adjacentes ou não se sobrepõem")
            void naoDeveConflitarQuandoAdjacenteOuSeparado(String inicioExistente, String fimExistente,
                                                           String inicioNovo, String fimNovo) {
                Horario existente = criarHorarioExistente(UUID.randomUUID(), DiaSemanaEnum.SEGUNDA,
                        LocalTime.parse(inicioExistente), LocalTime.parse(fimExistente));

                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA,
                        LocalTime.parse(inicioNovo), LocalTime.parse(fimNovo));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                        .thenReturn(List.of(existente));
                when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

                assertThatCode(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .doesNotThrowAnyException();

                verify(horarioRepository).save(any(Horario.class));
            }

            @Test
            @DisplayName("não deve conflitar com horário em dia da semana diferente, mesmo com mesmo intervalo")
            void naoDeveConflitarEmDiaDiferente() {
                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.TERCA, LocalTime.of(8, 0), LocalTime.of(10, 0));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.TERCA))
                        .thenReturn(List.of());
                when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

                assertThatCode(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("atualizarHorario")
    class AtualizarHorario {

        @Test
        @DisplayName("deve atualizar com sucesso ignorando conflito consigo mesmo")
        void deveAtualizarIgnorandoConflitoConsigoMesmo() {
            UUID idHorario = UUID.randomUUID();
            Horario horarioExistente = criarHorarioExistente(idHorario, DiaSemanaEnum.SEGUNDA,
                    LocalTime.of(8, 0), LocalTime.of(10, 0));

            HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 30));

            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorario, idUsuarioLogado))
                    .thenReturn(Optional.of(horarioExistente));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                    .thenReturn(List.of(horarioExistente));
            when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

            HorarioResponseDTO resultado = horarioService.atualizarHorario(idHorario, dto, idUsuarioLogado);

            assertThat(resultado.horaFim()).isEqualTo(LocalTime.of(10, 30));
        }

        @Test
        @DisplayName("deve lançar HorarioConflitanteException ao atualizar para um horário que conflita com OUTRO já existente")
        void deveLancarExceptionAoConflitarComOutro() {
            UUID idHorarioEditando = UUID.randomUUID();
            UUID idOutroHorario = UUID.randomUUID();

            Horario horarioEditando = criarHorarioExistente(idHorarioEditando, DiaSemanaEnum.SEGUNDA,
                    LocalTime.of(8, 0), LocalTime.of(9, 0));
            Horario outroHorario = criarHorarioExistente(idOutroHorario, DiaSemanaEnum.SEGUNDA,
                    LocalTime.of(14, 0), LocalTime.of(16, 0));

            HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(15, 0), LocalTime.of(17, 0));

            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorarioEditando, idUsuarioLogado))
                    .thenReturn(Optional.of(horarioEditando));
            when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                    .thenReturn(Optional.of(disciplinaExistente));
            when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                    .thenReturn(List.of(horarioEditando, outroHorario));

            assertThatThrownBy(() -> horarioService.atualizarHorario(idHorarioEditando, dto, idUsuarioLogado))
                    .isInstanceOf(HorarioConflitanteException.class);

            verify(horarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve permitir atualizar horário de outro usuário (IDOR)")
        void naoDevePermitirAtualizarDeOutroUsuario() {
            UUID idHorario = UUID.randomUUID();
            UUID idOutroUsuario = UUID.randomUUID();
            HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));

            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorario, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> horarioService.atualizarHorario(idHorario, dto, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(horarioRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("buscarHorarioPorId — IDOR")
    class BuscarHorarioPorId {

        @Test
        @DisplayName("deve retornar horário quando pertence ao usuário")
        void deveRetornarQuandoPertenceAoUsuario() {
            UUID idHorario = UUID.randomUUID();
            Horario horario = criarHorarioExistente(idHorario, DiaSemanaEnum.QUARTA, LocalTime.of(14, 0), LocalTime.of(16, 0));
            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorario, idUsuarioLogado))
                    .thenReturn(Optional.of(horario));

            HorarioResponseDTO resultado = horarioService.buscarHorarioPorId(idHorario, idUsuarioLogado);

            assertThat(resultado.idHora()).isEqualTo(idHorario);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando horário pertence a outro usuário")
        void deveLancarExceptionQuandoPertenceAOutroUsuario() {
            UUID idHorario = UUID.randomUUID();
            UUID idOutroUsuario = UUID.randomUUID();
            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorario, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> horarioService.buscarHorarioPorId(idHorario, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deletarHorario")
    class DeletarHorario {

        @Test
        @DisplayName("deve deletar com sucesso quando pertence ao usuário")
        void deveDeletarComSucesso() {
            UUID idHorario = UUID.randomUUID();
            Horario horario = criarHorarioExistente(idHorario, DiaSemanaEnum.SEXTA, LocalTime.of(10, 0), LocalTime.of(12, 0));
            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorario, idUsuarioLogado))
                    .thenReturn(Optional.of(horario));

            horarioService.deletarHorario(idHorario, idUsuarioLogado);

            verify(horarioRepository).delete(horario);
        }

        @Test
        @DisplayName("não deve permitir deletar horário de outro usuário")
        void naoDevePermitirDeletarDeOutroUsuario() {
            UUID idHorario = UUID.randomUUID();
            UUID idOutroUsuario = UUID.randomUUID();
            when(horarioRepository.findByIdHoraAndDisciplinaUsuarioIdUsuario(idHorario, idOutroUsuario))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> horarioService.deletarHorario(idHorario, idOutroUsuario))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(horarioRepository, never()).delete(any());
        }

        @Nested
        @DisplayName("conflito ignora horários de disciplinas arquivadas")
        class ConflitoComDisciplinaArquivada {

            @Test
            @DisplayName("deve permitir criar horário que coincide com horário de disciplina JÁ ARQUIVADA")
            void devePermitirQuandoConflitoEhComDisciplinaArquivada() {
                Disciplina disciplinaArquivada = new Disciplina();
                disciplinaArquivada.setIdDisciplina(UUID.randomUUID());
                disciplinaArquivada.setArquivada(true);

                Horario horarioAntigo = criarHorarioExistente(UUID.randomUUID(), DiaSemanaEnum.SEGUNDA,
                        LocalTime.of(8, 0), LocalTime.of(10, 0));
                horarioAntigo.setDisciplina(disciplinaArquivada);

                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                        .thenReturn(List.of());
                when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

                assertThatCode(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .doesNotThrowAnyException();

                verify(horarioRepository).save(any(Horario.class));
            }

            @Test
            @DisplayName("não deve conflitar com horário de disciplina ativa de OUTRO período")
            void naoDeveConflitarComOutroPeriodo() {
                disciplinaExistente.setPeriodo(2);

                Disciplina disciplinaDoPeriodoAnterior = new Disciplina();
                disciplinaDoPeriodoAnterior.setIdDisciplina(UUID.randomUUID());
                disciplinaDoPeriodoAnterior.setPeriodo(1);
                disciplinaDoPeriodoAnterior.setArquivada(false);

                Horario horarioDoPeriodoAnterior = criarHorarioExistente(UUID.randomUUID(), DiaSemanaEnum.SEGUNDA,
                        LocalTime.of(8, 0), LocalTime.of(10, 0));
                horarioDoPeriodoAnterior.setDisciplina(disciplinaDoPeriodoAnterior);

                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(10, 0));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                        .thenReturn(List.of(horarioDoPeriodoAnterior));
                when(horarioRepository.save(any(Horario.class))).thenAnswer(inv -> inv.getArgument(0));

                assertThatCode(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .doesNotThrowAnyException();

                verify(horarioRepository).save(any(Horario.class));
            }

            @Test
            @DisplayName("deve conflitar com horário de disciplina ativa do MESMO período")
            void deveConflitarComMesmoPeriodo() {
                disciplinaExistente.setPeriodo(2);

                Disciplina outraDisciplinaDoMesmoPeriodo = new Disciplina();
                outraDisciplinaDoMesmoPeriodo.setIdDisciplina(UUID.randomUUID());
                outraDisciplinaDoMesmoPeriodo.setPeriodo(2);
                outraDisciplinaDoMesmoPeriodo.setArquivada(false);

                Horario horarioDoMesmoPeriodo = criarHorarioExistente(UUID.randomUUID(), DiaSemanaEnum.SEGUNDA,
                        LocalTime.of(8, 0), LocalTime.of(10, 0));
                horarioDoMesmoPeriodo.setDisciplina(outraDisciplinaDoMesmoPeriodo);

                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(9, 0), LocalTime.of(11, 0));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                        .thenReturn(List.of(horarioDoMesmoPeriodo));

                assertThatThrownBy(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .isInstanceOf(HorarioConflitanteException.class);

                verify(horarioRepository, never()).save(any());
            }

            @Test
            @DisplayName("deve continuar conflitando com horário de disciplina ATIVA, mesmo com outras arquivadas no mesmo dia")
            void deveContinuarConflitandoComDisciplinaAtiva() {
                Horario horarioAtivo = criarHorarioExistente(UUID.randomUUID(), DiaSemanaEnum.SEGUNDA,
                        LocalTime.of(8, 0), LocalTime.of(10, 0));

                HorarioRequestDTO dto = criarDto(DiaSemanaEnum.SEGUNDA, LocalTime.of(9, 0), LocalTime.of(11, 0));

                when(disciplinaRepository.findByIdDisciplinaAndUsuarioIdUsuario(idDisciplina, idUsuarioLogado))
                        .thenReturn(Optional.of(disciplinaExistente));
                when(horarioRepository.findByDisciplinaUsuarioIdUsuarioAndDiaSemanaAndDisciplinaArquivadaFalse(idUsuarioLogado, DiaSemanaEnum.SEGUNDA))
                        .thenReturn(List.of(horarioAtivo));

                assertThatThrownBy(() -> horarioService.criarHorario(dto, idUsuarioLogado))
                        .isInstanceOf(HorarioConflitanteException.class);

                verify(horarioRepository, never()).save(any());
            }
        }
    }
}