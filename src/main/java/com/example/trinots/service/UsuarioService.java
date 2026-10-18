package com.example.trinots.service;


import com.example.trinots.domain.Curso;
import com.example.trinots.domain.Usuario;
import com.example.trinots.dto.AuthDTO.TrocarSenhaDTO;
import com.example.trinots.dto.CursoDTO.CursoRequestDTO;
import com.example.trinots.dto.CursoDTO.CursoResponseDTO;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.dto.UsuarioDTO.ExcluirContaDTO;
import com.example.trinots.dto.UsuarioDTO.NovaMediaDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioRequestDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioResponseDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioUpdateDTO;
import com.example.trinots.exception.exceptions.CredenciaisInvalidasException;
import com.example.trinots.exception.exceptions.EmailJaCadastradoException;
import com.example.trinots.repository.AvaliacaoRepository;
import com.example.trinots.repository.CodigoVerificacaoRepository;
import com.example.trinots.repository.CursoRepository;
import com.example.trinots.repository.DisciplinaRepository;
import com.example.trinots.repository.HorarioRepository;
import com.example.trinots.repository.TarefaRepository;
import com.example.trinots.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final PasswordEncoder passwordEncoder;
    private final DisciplinaRepository disciplinaRepository;
    private final DisciplinaService disciplinaService;
    private final VerificacaoService verificacaoService;
    private final CodigoVerificacaoRepository codigoVerificacaoRepository;
    private final TarefaRepository tarefaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final HorarioRepository horarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, CursoRepository cursoRepository, PasswordEncoder passwordEncoder,
                          DisciplinaRepository disciplinaRepository, DisciplinaService disciplinaService,
                          VerificacaoService verificacaoService, CodigoVerificacaoRepository codigoVerificacaoRepository,
                          TarefaRepository tarefaRepository, AvaliacaoRepository avaliacaoRepository,
                          HorarioRepository horarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.passwordEncoder = passwordEncoder;
        this.disciplinaRepository = disciplinaRepository;
        this.disciplinaService = disciplinaService;
        this.verificacaoService = verificacaoService;
        this.codigoVerificacaoRepository = codigoVerificacaoRepository;
        this.tarefaRepository = tarefaRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.horarioRepository = horarioRepository;
    }

    public UsuarioResponseDTO criarUsuario(UsuarioRequestDTO dto) {
        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        Curso curso = new Curso();
        curso.setNomeCurso(dto.curso().nomeCurso());
        curso.setInstituicao(dto.curso().instituicao());
        curso.setDuracao(dto.curso().duracao());
        Curso cursoSalvo = cursoRepository.save(curso);

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setSobrenome(dto.sobrenome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setCurso(cursoSalvo);
        usuario.setSemestreAtual(dto.semestreAtual());

        Usuario salvo = usuarioRepository.save(usuario);
        verificacaoService.gerarEEnviar(salvo, TipoCodigoEnum.VERIFICACAO_EMAIL);

        return toResponseDTO(salvo);
    }

    public UsuarioResponseDTO atualizarUsuario(UUID id, UsuarioUpdateDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        usuario.setNome(dto.nome());
        usuario.setSobrenome(dto.sobrenome());
        usuario.setSemestreAtual(dto.semestreAtual());

        Usuario atualizado = usuarioRepository.save(usuario);
        return toResponseDTO(atualizado);
    }

    public CursoResponseDTO atualizarCurso(UUID id, CursoRequestDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);
        Curso curso = buscarCursoPorId(usuario.getCurso().getIdCurso());

        curso.setNomeCurso(dto.nomeCurso());
        curso.setInstituicao(dto.instituicao());
        curso.setDuracao(dto.duracao());

        cursoRepository.save(curso);

        return new CursoResponseDTO(
                usuario.getCurso().getIdCurso(),
                usuario.getCurso().getNomeCurso(),
                usuario.getCurso().getInstituicao(),
                usuario.getCurso().getDuracao()
        );
    }

    public void trocarSenha(UUID id, TrocarSenhaDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Senha atual incorreta");
        }

        if (!dto.novaSenha().equals(dto.novaSenhaConfirmada())) {
            throw new CredenciaisInvalidasException("Novas senhas diferentes");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    public UsuarioResponseDTO buscarUsuarioPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        return toResponseDTO(usuario);
    }

    private Usuario buscarEntidadePorId(UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    private Curso buscarCursoPorId(UUID id) {
        return cursoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Curso não encontrado"));
    }


    public Double buscarMediaAprovacao(UUID idUsuarioLogado) {
        Usuario usuario = usuarioRepository.findById(idUsuarioLogado).orElseThrow(
                () -> new EntityNotFoundException("Usuário não encontrado.")
        );

        return usuario.getMediaAprovacao();
    }

    public void alterarMediaAprovacao(UUID idUsuarioLogado, NovaMediaDTO dto) {
        Usuario usuario = usuarioRepository.findById(idUsuarioLogado).orElseThrow(
                () -> new EntityNotFoundException("Usuário não encontrado.")
        );

        usuario.setMediaAprovacao(dto.novaMedia());
        usuarioRepository.save(usuario);
        disciplinaService.recalcularStatusDeTodasAsDisciplinas(idUsuarioLogado);
    }

    /** Desativa a conta (reversível via /auth/reativar). O token atual deixa de ser aceito. */
    public void desativarUsuario(UUID id) {
        Usuario usuario = buscarEntidadePorId(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    /** Exclui a conta e todos os dados do usuário em cascata. Exige a senha. */
    public void excluirConta(UUID id, ExcluirContaDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        if (!passwordEncoder.matches(dto.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Senha incorreta");
        }

        Curso curso = usuario.getCurso();

        avaliacaoRepository.deleteByDisciplinaUsuarioIdUsuario(id);
        tarefaRepository.deleteByDisciplinaUsuarioIdUsuario(id);
        horarioRepository.deleteByDisciplinaUsuarioIdUsuario(id);
        disciplinaRepository.deleteByUsuarioIdUsuario(id);
        codigoVerificacaoRepository.deleteByUsuarioIdUsuario(id);
        usuarioRepository.delete(usuario);
        if (curso != null) {
            cursoRepository.delete(curso);
        }
    }

    private UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        CursoResponseDTO cursoDTO = new CursoResponseDTO(
                usuario.getCurso().getIdCurso(),
                usuario.getCurso().getNomeCurso(),
                usuario.getCurso().getInstituicao(),
                usuario.getCurso().getDuracao()
        );
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getSobrenome(),
                usuario.getEmail(),
                cursoDTO,
                usuario.getSemestreAtual(),
                usuario.isAtivo(),
                usuario.isEmailVerificado()
        );
    }

}
