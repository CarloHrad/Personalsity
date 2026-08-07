package com.example.trinots.controller;

import com.example.trinots.domain.Usuario;
import com.example.trinots.dto.AuthDTO.TrocarSenhaDTO;
import com.example.trinots.dto.CursoDTO.CursoRequestDTO;
import com.example.trinots.dto.CursoDTO.CursoResponseDTO;
import com.example.trinots.dto.UsuarioDTO.NovaMediaDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioRequestDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioResponseDTO;
import com.example.trinots.dto.UsuarioDTO.UsuarioUpdateDTO;
import com.example.trinots.service.AuthService;
import com.example.trinots.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO usuarioCriado = usuarioService.criarUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado);
    }

    @PatchMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal Usuario usuarioLogado, @Valid @RequestBody TrocarSenhaDTO dto) {
        usuarioService.trocarSenha(usuarioLogado.getIdUsuario(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> buscarPerfil(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarUsuarioPorId(usuarioLogado.getIdUsuario()));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@AuthenticationPrincipal Usuario usuarioLogado, @Valid @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarUsuario(usuarioLogado.getIdUsuario(), dto));
    }

    @PutMapping("/curso")
    public ResponseEntity<CursoResponseDTO> atualizarCurso(@AuthenticationPrincipal Usuario usuarioLogado, @Valid @RequestBody CursoRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarCurso(usuarioLogado.getIdUsuario(), dto));
    }

    @GetMapping("/configuracoes/media")
    public ResponseEntity<Double> buscarMediaAprovacao(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarMediaAprovacao(usuarioLogado.getIdUsuario()));
    }
    
    //Consertar
    @PutMapping("/configuracoes/media")
    public ResponseEntity<Void> alterarMediaAprovacao(@AuthenticationPrincipal Usuario usuarioLogado, @Valid @RequestBody NovaMediaDTO novaMedia) {
        usuarioService.alterarMediaAprovacao(usuarioLogado.getIdUsuario(), novaMedia);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/desativar")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal Usuario usuarioLogado) {
        usuarioService.desativarUsuario(usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/ativar")
    public ResponseEntity<Void> ativar(@AuthenticationPrincipal Usuario usuarioLogado) {
        usuarioService.ativarUsuario(usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirConta(@AuthenticationPrincipal Usuario usuarioLogado) {
        usuarioService.excluirConta(usuarioLogado);
        return ResponseEntity.noContent().build();
    }
}