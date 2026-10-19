package com.example.trinots.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressao do "401 virando 500": o corpo de erro dos handlers de seguranca precisa ser JSON valido
 * e nao pode depender de modulos do Jackson. Teste puro (sem Spring).
 */
class RespostaErroJsonTest {

    private static com.fasterxml.jackson.databind.JsonNode ler(String json) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
    }

    @Test
    void formatoIgualAoErrorResponseDTO() throws Exception {
        var n = ler(RespostaErroJson.montar("401", "Autenticação necessária", LocalDateTime.of(2030, 1, 2, 3, 4, 5)));
        assertEquals("401", n.get("codigo").asText());
        assertEquals("Autenticação necessária", n.get("mensagem").asText());
        assertEquals("2030-01-02T03:04:05", n.get("timestamp").asText());
        assertEquals(3, n.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"com \"aspas\"", "barra \\ invertida", "quebra\nde linha", "tab\taqui", "controle\u0001x",
            "unicode ção 😀 李", "</script>", "{\"injetado\":true}", ""})
    void mensagemComCaracteresEspeciaisContinuaJsonValido(String mensagem) throws Exception {
        var n = ler(RespostaErroJson.montar("403", mensagem, LocalDateTime.now()));
        assertEquals(mensagem, n.get("mensagem").asText());
        assertEquals(3, n.size(), "a mensagem nao pode injetar campos novos");
    }

    @Test
    void timestampPresenteEIso() throws Exception {
        String ts = ler(RespostaErroJson.montar("401", "x", LocalDateTime.now())).get("timestamp").asText();
        assertTrue(ts.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"), ts);
    }

    @Test
    void mensagemNulaViraNullJson() throws Exception {
        assertTrue(ler(RespostaErroJson.montar("401", null, LocalDateTime.now())).get("mensagem").isNull());
    }
}
