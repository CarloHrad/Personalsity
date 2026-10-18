package com.example.trinots.controller;

import com.example.trinots.dto.AuthDTO.*;
import com.example.trinots.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.autenticar(dto));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> solicitarRecuperacao(@Valid @RequestBody SolicitarRecuperacaoDTO dto) {
        authService.solicitarRecuperacaoSenha(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaComTokenDTO dto) {
        authService.redefinirSenhaComToken(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<Void> verificarEmail(@Valid @RequestBody VerificarEmailDTO dto) {
        authService.verificarEmail(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<Void> reenviarVerificacao(@Valid @RequestBody ReenviarVerificacaoDTO dto) {
        authService.reenviarVerificacao(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reativar")
    public ResponseEntity<Void> reativarConta(@Valid @RequestBody LoginRequestDTO dto) {
        authService.reativarConta(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}