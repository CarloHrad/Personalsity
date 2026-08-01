package com.example.trinots.controller;

import com.example.trinots.domain.Usuario;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoConcluirDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoRequestDTO;
import com.example.trinots.dto.AvaliacaoDTO.AvaliacaoResponseDTO;
import com.example.trinots.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;


    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }


    @PostMapping
    public ResponseEntity<AvaliacaoResponseDTO> criar(@Valid @RequestBody AvaliacaoRequestDTO dto,
                                                      @AuthenticationPrincipal Usuario usuarioLogado) {
        AvaliacaoResponseDTO criada = avaliacaoService.criarAvaliacao(dto, usuarioLogado.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }


    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoResponseDTO> buscarPorId(@PathVariable UUID id,
                                                            @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avaliacaoService.buscarAvaliacaoPorId(id, usuarioLogado.getIdUsuario()));
    }


    @GetMapping("/disciplina/{idDisciplina}")
    public ResponseEntity<List<AvaliacaoResponseDTO>> listarPorDisciplina(@PathVariable UUID idDisciplina,
                                                                          @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avaliacaoService.listarPorDisciplina(idDisciplina, usuarioLogado.getIdUsuario()));
    }


    @PutMapping("/{id}")
    public ResponseEntity<AvaliacaoResponseDTO> atualizar(@PathVariable UUID id,
                                                          @Valid @RequestBody AvaliacaoRequestDTO dto,
                                                          @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avaliacaoService.atualizarAvaliacao(id, dto, usuarioLogado.getIdUsuario()));
    }


    @PatchMapping("/{id}/concluir")
    public ResponseEntity<AvaliacaoResponseDTO> concluir(@PathVariable UUID id,
                                                         @Valid @RequestBody AvaliacaoConcluirDTO dto,
                                                         @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avaliacaoService.concluirAvaliacao(id, dto, usuarioLogado.getIdUsuario()));
    }


    @PatchMapping("/{id}/reabrir")
    public ResponseEntity<AvaliacaoResponseDTO> reabrir(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avaliacaoService.reabrirAvaliacao(id, usuarioLogado.getIdUsuario()));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        avaliacaoService.deletarAvaliacao(id, usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}