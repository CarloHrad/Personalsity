package com.example.trinots.UsuarioServiceTest;

import com.example.trinots.domain.Curso;
import com.example.trinots.domain.Usuario;
import com.example.trinots.dto.CursoDTO.CursoRequestDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioRequestDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioResponseDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioUpdateDTO;
import com.example.trinots.dto.AuthDTO.TrocarSenhaDTO;
import com.example.trinots.exception.exceptions.CredenciaisInvalidasException;
import com.example.trinots.exception.exceptions.EmailJaCadastradoException;
import com.example.trinots.repository.CursoRepository;
import com.example.trinots.repository.UsuarioRepository;
import com.example.trinots.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private CursoRepository cursoRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;

    private UUID usuarioId;
    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        cursoRepository = mock(CursoRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        usuarioService = new UsuarioService(usuarioRepository, cursoRepository, passwordEncoder);

        usuarioId = UUID.randomUUID();

        Curso curso = new Curso();
        curso.setIdCurso(UUID.randomUUID());
        curso.setNomeCurso("Análise e Desenvolvimento de Sistemas");
        curso.setInstituicao("Faculdade XYZ");
        curso.setDuracao(6);

        usuarioExistente = new Usuario();
        usuarioExistente.setIdUsuario(usuarioId);
        usuarioExistente.setNome("João");
        usuarioExistente.setSobrenome("Silva");
        usuarioExistente.setEmail("joao@email.com");
        usuarioExistente.setSenha("senhaHasheada");
        usuarioExistente.setCurso(curso);
        usuarioExistente.setSemestreAtual(3);
        usuarioExistente.setDataNascimento(LocalDate.of(2000, 5, 10));
        usuarioExistente.setAtivo(true);
    }

    private UsuarioRequestDTO criarDtoValido() {
        CursoRequestDTO cursoDto = new CursoRequestDTO("ADS", "Faculdade XYZ", 6);
        return new UsuarioRequestDTO(
                "João", "Silva", "joao@email.com", "senha123",
                cursoDto, 3, LocalDate.of(2000, 5, 10)
        );
    }

    @Nested
    @DisplayName("criarUsuario")
    class CriarUsuario {

        @Test
        @DisplayName("deve criar usuário com sucesso quando email não existe")
        void deveCriarUsuarioComSucesso() {
            UsuarioRequestDTO dto = criarDtoValido();

            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
            when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
                Curso c = invocation.getArgument(0);
                c.setIdCurso(UUID.randomUUID());
                return c;
            });
            when(passwordEncoder.encode(dto.senha())).thenReturn("senhaHasheada");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UsuarioResponseDTO resultado = usuarioService.criarUsuario(dto);

            assertThat(resultado).isNotNull();
            assertThat(resultado.nome()).isEqualTo("João");
            assertThat(resultado.email()).isEqualTo("joao@email.com");
            verify(passwordEncoder).encode("senha123");
            verify(cursoRepository).save(any(Curso.class));
            verify(usuarioRepository).save(any(Usuario.class));
        }

        @Test
        @DisplayName("deve lançar EmailJaCadastradoException quando email já existe")
        void deveLancarExceptionQuandoEmailJaExiste() {
            UsuarioRequestDTO dto = criarDtoValido();
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.of(usuarioExistente));

            assertThatThrownBy(() -> usuarioService.criarUsuario(dto))
                    .isInstanceOf(EmailJaCadastradoException.class)
                    .hasMessageContaining("Email já cadastrado");

            verify(cursoRepository, never()).save(any());
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("deve salvar senha criptografada, nunca a senha em texto puro")
        void deveSalvarSenhaCriptografada() {
            UsuarioRequestDTO dto = criarDtoValido();
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
            when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));
            when(passwordEncoder.encode("senha123")).thenReturn("HASH_SEGURO");

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            when(usuarioRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.criarUsuario(dto);

            assertThat(captor.getValue().getSenha()).isEqualTo("HASH_SEGURO");
            assertThat(captor.getValue().getSenha()).isNotEqualTo("senha123");
        }

        @Test
        @DisplayName("deve criar o Curso vinculado com os dados do DTO aninhado")
        void deveCriarCursoComDadosCorretos() {
            UsuarioRequestDTO dto = criarDtoValido();
            when(usuarioRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("hash");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Curso> cursoCaptor = ArgumentCaptor.forClass(Curso.class);
            when(cursoRepository.save(cursoCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.criarUsuario(dto);

            assertThat(cursoCaptor.getValue().getNomeCurso()).isEqualTo("ADS");
            assertThat(cursoCaptor.getValue().getInstituicao()).isEqualTo("Faculdade XYZ");
            assertThat(cursoCaptor.getValue().getDuracao()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("buscarUsuarioPorId")
    class BuscarUsuarioPorId {

        @Test
        @DisplayName("deve retornar usuário quando id existe")
        void deveRetornarUsuarioQuandoIdExiste() {
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));

            UsuarioResponseDTO resultado = usuarioService.buscarUsuarioPorId(usuarioId);

            assertThat(resultado.idUsuario()).isEqualTo(usuarioId);
            assertThat(resultado.email()).isEqualTo("joao@email.com");
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando id não existe")
        void deveLancarExceptionQuandoIdNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.buscarUsuarioPorId(idInexistente))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }
    }

    @Nested
    @DisplayName("atualizarUsuario")
    class AtualizarUsuario {

        @Test
        @DisplayName("deve atualizar nome, sobrenome e semestre com sucesso")
        void deveAtualizarComSucesso() {
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("João Pedro", "Silva Santos", 4);
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resultado = usuarioService.atualizarUsuario(usuarioId, dto);

            assertThat(resultado.nome()).isEqualTo("João Pedro");
            assertThat(resultado.sobrenome()).isEqualTo("Silva Santos");
            assertThat(resultado.semestreAtual()).isEqualTo(4);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException ao atualizar usuário inexistente")
        void deveLancarExceptionAoAtualizarInexistente() {
            UUID idInexistente = UUID.randomUUID();
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Nome", "Sobrenome", 2);
            when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.atualizarUsuario(idInexistente, dto))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("não deve alterar email ou senha ao atualizar perfil")
        void naoDeveAlterarEmailOuSenha() {
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Novo Nome", "Novo Sobrenome", 5);
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.atualizarUsuario(usuarioId, dto);

            assertThat(usuarioExistente.getEmail()).isEqualTo("joao@email.com");
            assertThat(usuarioExistente.getSenha()).isEqualTo("senhaHasheada");
        }
    }

    @Nested
    @DisplayName("trocarSenha")
    class TrocarSenha {

        @Test
        @DisplayName("deve trocar senha com sucesso quando senha atual está correta")
        void deveTrocarSenhaComSucesso() {
            TrocarSenhaDTO dto = new TrocarSenhaDTO("senhaAtual123", "novaSenha456");
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
            when(passwordEncoder.matches("senhaAtual123", "senhaHasheada")).thenReturn(true);
            when(passwordEncoder.encode("novaSenha456")).thenReturn("novaHash");

            usuarioService.trocarSenha(usuarioId, dto);

            assertThat(usuarioExistente.getSenha()).isEqualTo("novaHash");
            verify(usuarioRepository).save(usuarioExistente);
        }

        @Test
        @DisplayName("deve lançar CredenciaisInvalidasException quando senha atual está incorreta")
        void deveLancarExceptionQuandoSenhaAtualIncorreta() {
            TrocarSenhaDTO dto = new TrocarSenhaDTO("senhaErrada", "novaSenha456");
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));
            when(passwordEncoder.matches("senhaErrada", "senhaHasheada")).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.trocarSenha(usuarioId, dto))
                    .isInstanceOf(CredenciaisInvalidasException.class)
                    .hasMessageContaining("Senha atual incorreta");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando usuário não existe")
        void deveLancarExceptionQuandoUsuarioNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            TrocarSenhaDTO dto = new TrocarSenhaDTO("qualquer", "novaSenha456");
            when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.trocarSenha(idInexistente, dto))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("desativarUsuario")
    class DesativarUsuario {

        @Test
        @DisplayName("deve desativar usuário ativo com sucesso")
        void deveDesativarComSucesso() {
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));

            usuarioService.desativarUsuario(usuarioId);

            assertThat(usuarioExistente.isAtivo()).isFalse();
            verify(usuarioRepository).save(usuarioExistente);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException ao desativar usuário inexistente")
        void deveLancarExceptionAoDesativarInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.desativarUsuario(idInexistente))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve permitir desativar um usuário já desativado sem erro (idempotente)")
        void deveSerIdempotente() {
            usuarioExistente.setAtivo(false);
            when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioExistente));

            usuarioService.desativarUsuario(usuarioId);

            assertThat(usuarioExistente.isAtivo()).isFalse();
            verify(usuarioRepository).save(usuarioExistente);
        }
    }
}