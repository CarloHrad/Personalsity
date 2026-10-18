package com.example.trinots.FusoHorarioServiceTest;

import com.example.trinots.service.FusoHorarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FusoHorarioService")
class FusoHorarioServiceTest {

    private final FusoHorarioService fusoHorarioService = new FusoHorarioService();

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void requisicaoComHeader(String valor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (valor != null) {
            request.addHeader(FusoHorarioService.HEADER, valor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("usa o fuso enviado pelo aparelho no header X-Timezone")
    void usaFusoDoHeader() {
        requisicaoComHeader("Asia/Tokyo");

        assertThat(fusoHorarioService.fusoAtual()).isEqualTo(ZoneId.of("Asia/Tokyo"));
    }

    @Test
    @DisplayName("header inválido cai no fuso padrão, sem lançar exceção")
    void headerInvalidoUsaPadrao() {
        requisicaoComHeader("Isso/NaoExiste");

        assertThat(fusoHorarioService.fusoAtual()).isEqualTo(FusoHorarioService.FUSO_PADRAO);
    }

    @Test
    @DisplayName("sem header, usa o fuso padrão (America/Sao_Paulo)")
    void semHeaderUsaPadrao() {
        requisicaoComHeader(null);

        assertThat(fusoHorarioService.fusoAtual()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    @DisplayName("fora de uma requisição HTTP, usa o fuso padrão")
    void foraDeRequisicaoUsaPadrao() {
        assertThat(fusoHorarioService.fusoAtual()).isEqualTo(FusoHorarioService.FUSO_PADRAO);
    }

    @Test
    @DisplayName("hoje() e agora() respeitam o fuso do header")
    void hojeEAgoraRespeitamFuso() {
        requisicaoComHeader("Pacific/Kiritimati");

        assertThat(fusoHorarioService.agora().toLocalDate()).isEqualTo(fusoHorarioService.hoje());
        assertThat(fusoHorarioService.hoje()).isEqualTo(java.time.LocalDate.now(ZoneId.of("Pacific/Kiritimati")));
    }
}
