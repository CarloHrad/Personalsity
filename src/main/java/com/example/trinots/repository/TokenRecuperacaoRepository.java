package com.example.trinots.repository;

import com.example.trinots.domain.extra.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacaoRepository extends JpaRepository<TokenRecuperacaoSenha, UUID> {
    Optional<TokenRecuperacaoSenha> findByToken(String token);
}