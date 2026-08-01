package com.example.trinots.controller;

import com.example.trinots.domain.Usuario;
import com.example.trinots.dto.TarefaDTO.TarefaRequestDTO;
import com.example.trinots.dto.TarefaDTO.TarefaResponseDTO;
import com.example.trinots.service.TarefaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;


    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }


    @PostMapping
    public ResponseEntity<TarefaResponseDTO> criar(@Valid @RequestBody TarefaRequestDTO dto,
                                                   @AuthenticationPrincipal Usuario usuarioLogado) {
        TarefaResponseDTO criada = tarefaService.criarTarefa(dto, usuarioLogado.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }


    @GetMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> buscarPorId(@PathVariable UUID id,
                                                         @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(tarefaService.buscarTarefaPorId(id, usuarioLogado.getIdUsuario()));
    }


    @GetMapping("/disciplina/{idDisciplina}")
    public ResponseEntity<List<TarefaResponseDTO>> listarPorDisciplina(@PathVariable UUID idDisciplina,
                                                                       @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(tarefaService.listarPorDisciplina(idDisciplina, usuarioLogado.getIdUsuario()));
    }


    @GetMapping("/pendentes")
    public ResponseEntity<List<TarefaResponseDTO>> listarPendentes(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(tarefaService.listarPendentes(usuarioLogado.getIdUsuario()));
    }


    @PutMapping("/{id}")
    public ResponseEntity<TarefaResponseDTO> atualizar(@PathVariable UUID id,
                                                       @Valid @RequestBody TarefaRequestDTO dto,
                                                       @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(tarefaService.atualizarTarefa(id, dto, usuarioLogado.getIdUsuario()));
    }


    @PatchMapping("/{id}/concluir")
    public ResponseEntity<TarefaResponseDTO> concluir(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(tarefaService.concluirTarefa(id, usuarioLogado.getIdUsuario()));
    }


    @PatchMapping("/{id}/reabrir")
    public ResponseEntity<TarefaResponseDTO> reabrir(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(tarefaService.reabrirTarefa(id, usuarioLogado.getIdUsuario()));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        tarefaService.deletarTarefa(id, usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}