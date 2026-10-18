package com.example.trinots.repository;

import com.example.trinots.domain.enums.TipoCodigoEnum;
import com.example.trinots.domain.extra.CodigoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, UUID> {

    List<CodigoVerificacao> findByUsuarioIdUsuarioAndTipoAndUsadoFalse(UUID idUsuario, TipoCodigoEnum tipo);

    Optional<CodigoVerificacao> findFirstByUsuarioIdUsuarioAndTipoAndUsadoFalseOrderByDataExpiracaoDesc(
            UUID idUsuario, TipoCodigoEnum tipo);

    void deleteByUsuarioIdUsuario(UUID idUsuario);

    void deleteByDataExpiracaoBefore(Instant limite);
}
