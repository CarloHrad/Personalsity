package com.example.trinots.controller;

import com.example.trinots.domain.Usuario;
import com.example.trinots.dto.HorarioDTO.HorarioRequestDTO;
import com.example.trinots.dto.HorarioDTO.HorarioResponseDTO;
import com.example.trinots.service.HorarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/horarios")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @PostMapping
    public ResponseEntity<HorarioResponseDTO> criar(@Valid @RequestBody HorarioRequestDTO dto,
                                                    @AuthenticationPrincipal Usuario usuarioLogado) {
        HorarioResponseDTO criado = horarioService.criarHorario(dto, usuarioLogado.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorarioResponseDTO> buscarPorId(@PathVariable UUID id,
                                                          @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(horarioService.buscarHorarioPorId(id, usuarioLogado.getIdUsuario()));
    }

    @GetMapping("/disciplina/{idDisciplina}")
    public ResponseEntity<List<HorarioResponseDTO>> listarPorDisciplina(@PathVariable UUID idDisciplina,
                                                                        @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(horarioService.listarPorDisciplina(idDisciplina, usuarioLogado.getIdUsuario()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorarioResponseDTO> atualizar(@PathVariable UUID id,
                                                        @Valid @RequestBody HorarioRequestDTO dto,
                                                        @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(horarioService.atualizarHorario(id, dto, usuarioLogado.getIdUsuario()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        horarioService.deletarHorario(id, usuarioLogado.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}