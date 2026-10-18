package com.example.trinots.service;

import com.example.trinots.repository.CodigoVerificacaoRepository;
import com.example.trinots.repository.TokenRevogadoRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Remove registros que já não têm utilidade: tokens revogados cujo JWT já teria expirado
 * e códigos de verificação vencidos. Roda todo dia às 03:00 (horário do servidor).
 */
@Service
@Slf4j
public class LimpezaAgendadaService {

    private final TokenRevogadoRepository tokenRevogadoRepository;
    private final CodigoVerificacaoRepository codigoVerificacaoRepository;

    public LimpezaAgendadaService(TokenRevogadoRepository tokenRevogadoRepository,
                                  CodigoVerificacaoRepository codigoVerificacaoRepository) {
        this.tokenRevogadoRepository = tokenRevogadoRepository;
        this.codigoVerificacaoRepository = codigoVerificacaoRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limparExpirados() {
        Instant agora = Instant.now();
        tokenRevogadoRepository.deleteByDataExpiracaoBefore(agora);
        codigoVerificacaoRepository.deleteByDataExpiracaoBefore(agora);
        log.info("Limpeza de tokens revogados e códigos expirados concluída");
    }
}
