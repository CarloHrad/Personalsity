package com.example.trinots.controller;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.StatusDisciplinaEnum;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaRequestDTO;
import com.example.trinots.dto.DisciplinaDTO.DisciplinaResponseDTO;
import com.example.trinots.service.DisciplinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/disciplinas")
public class DisciplinaController {

    private final DisciplinaService disciplinaService;

    public DisciplinaController(DisciplinaService disciplinaService) {
        this.disciplinaService = disciplinaService;
    }

    @PostMapping
    public ResponseEntity<DisciplinaResponseDTO> criar(@Valid @RequestBody DisciplinaRequestDTO dto,
                                                       @AuthenticationPrincipal Usuario usuarioLogado) {
        DisciplinaResponseDTO criada = disciplinaService.criarDisciplina(dto, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaResponseDTO> buscarPorId(@PathVariable UUID id,
                                                             @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(disciplinaService.buscarDisciplinaPorId(id, usuarioLogado.getIdUsuario()));
    }

    @GetMapping
    public ResponseEntity<List<DisciplinaResponseDTO>> listar(
            @AuthenticationPrincipal Usuario usuarioLogado, @RequestParam(required = false) StatusDisciplinaEnum status) {

        return ResponseEntity.ok(disciplinaService.listarDisciplinas(usuarioLogado, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisciplinaResponseDTO> atualizar(@PathVariable UUID id,
                                                           @Valid @RequestBody DisciplinaRequestDTO dto,
                                                           @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(disciplinaService.atualizarDisciplina(id, dto, usuarioLogado.getIdUsuario()));
    }

    @PatchMapping("/{id}/arquivar")
    public ResponseEntity<Void> arquivar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        disciplinaService.arquivarDisciplina(id, usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DisciplinaResponseDTO> atualizarStatus(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(disciplinaService.atualizarStatusDisciplina(id, usuarioLogado.getIdUsuario()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        disciplinaService.deletarDisciplina(id, usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}