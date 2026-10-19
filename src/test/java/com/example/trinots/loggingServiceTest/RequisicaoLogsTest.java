package com.example.trinots.loggingServiceTest;

import com.example.trinots.logging.RequisicaoLogs;
import com.example.trinots.logging.RequisicaoLogs.Nivel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RequisicaoLogsTest {

    // ------------------------------------------------------------- sanitizar
    @Test
    void quebraDeLinhaNaoPodeForjarEntradaNoLog() {
        String s = RequisicaoLogs.sanitizar("/x\r\nERROR falso user=admin", 200);
        assertFalse(s.contains("\n") || s.contains("\r"), s);
        assertEquals("/x__ERROR falso user=admin", s);
    }

    @Test
    void controlesUnicodeETabulacaoSaoNeutralizados() {
        String s = RequisicaoLogs.sanitizar("a\tb\u0000c\u001bd\u0085e f g", 50);
        assertEquals("a_b_c_d_e_f_g", s);
    }

    @Test
    void textoLongoEhTruncado() {
        assertEquals(10, RequisicaoLogs.sanitizar("a".repeat(5000), 10).length());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void nuloOuVazioViraTraco(String v) {
        assertEquals("-", RequisicaoLogs.sanitizar(v, 10));
    }

    @Test
    void acentosENormalmenteMantidos() {
        assertEquals("/disciplinas/ação", RequisicaoLogs.sanitizar("/disciplinas/ação", 50));
    }

    // ------------------------------------------------------------- requestId
    @ParameterizedTest
    @ValueSource(strings = {"abc12345", "req-2026.10_19-AB", "0123456789abcdef0123456789abcdef"})
    void requestIdSeguroEhAceito(String id) {
        assertEquals(id, RequisicaoLogs.requestIdValido(id));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "curto", "tem espaco 123", "abc\r\nINFO falso", "<script>alert(1)</script>",
            "abc12345'; DROP TABLE x;--", "ação-12345678",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void requestIdPerigosoOuForaDoPadraoEhRejeitado(String id) {
        assertNull(RequisicaoLogs.requestIdValido(id));
    }

    @Test
    void requestIdGeradoEhValidoEDiferenteACadaChamada() {
        String a = RequisicaoLogs.novoRequestId();
        String b = RequisicaoLogs.novoRequestId();
        assertEquals(12, a.length());
        assertNotNull(RequisicaoLogs.requestIdValido(a));
        assertNotEquals(a, b);
    }

    // ------------------------------------------------------------- IP
    @Test
    void usaOPrimeiroIpDoXForwardedFor() {
        assertEquals("200.1.2.3", RequisicaoLogs.ipDoCliente("200.1.2.3, 10.0.0.1, 10.0.0.2", "10.9.9.9"));
    }

    @Test
    void aceitaIpv6() {
        assertEquals("2804:14d:1::1", RequisicaoLogs.ipDoCliente("2804:14d:1::1", "10.9.9.9"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "nao-e-ip", "1.2.3.4\r\nERROR falso", "<script>", "' OR 1=1--"})
    void xForwardedForInvalidoCaiParaOEnderecoDaConexao(String header) {
        assertEquals("10.9.9.9", RequisicaoLogs.ipDoCliente(header, "10.9.9.9"));
    }

    // ------------------------------------------------------------- nivel
    @Test
    void niveisPorStatus() {
        assertEquals(Nivel.INFO, RequisicaoLogs.nivelPara(200));
        assertEquals(Nivel.INFO, RequisicaoLogs.nivelPara(201));
        assertEquals(Nivel.INFO, RequisicaoLogs.nivelPara(204));
        assertEquals(Nivel.INFO, RequisicaoLogs.nivelPara(400));
        assertEquals(Nivel.INFO, RequisicaoLogs.nivelPara(404));
        assertEquals(Nivel.WARN, RequisicaoLogs.nivelPara(401));
        assertEquals(Nivel.WARN, RequisicaoLogs.nivelPara(403));
        assertEquals(Nivel.ERROR, RequisicaoLogs.nivelPara(500));
        assertEquals(Nivel.ERROR, RequisicaoLogs.nivelPara(503));
    }

    // ------------------------------------------------------------- linha
    @Test
    void formatoDaLinha() {
        assertEquals("POST /disciplinas 201 35ms ip=200.1.2.3 user=f7eaa020-3c5d-4083-9cb4-85624a3d5eae",
                RequisicaoLogs.linha("POST", "/disciplinas", 201, 35, "200.1.2.3", "f7eaa020-3c5d-4083-9cb4-85624a3d5eae"));
    }

    @Test
    void usuarioAnonimoViraTraco() {
        assertEquals("POST /auth/login 401 12ms ip=1.2.3.4 user=-",
                RequisicaoLogs.linha("POST", "/auth/login", 401, 12, "1.2.3.4", null));
    }

    @Test
    void linhaSempreEmUmaSoLinhaMesmoComEntradaMaliciosa() {
        String l = RequisicaoLogs.linha("GE\nT", "/a\r\nWARN falso", 200, 1, "1.1.1.1\n", "u\nid");
        assertFalse(l.contains("\n") || l.contains("\r"), l);
    }
}
