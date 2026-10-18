package com.example.trinots.service;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envio de e-mails transacionais. Falhas de envio sao registradas, mas nao derrubam a requisicao:
 * o usuario pode pedir um novo codigo.
 *
 * Ha tres formas de envio, nesta ordem de prioridade:
 * <ol>
 *   <li><b>API HTTPS (Resend)</b>, quando {@code app.mail.resend.api-key} esta definida. E a opcao para
 *       hospedagens que bloqueiam SMTP (o Railway bloqueia nos planos Free, Trial e Hobby).</li>
 *   <li><b>SMTP</b>, quando {@code spring.mail.host} esta configurado.</li>
 *   <li><b>Somente log</b>, em desenvolvimento, com {@code app.mail.log-codigo=true}.
 *       Nunca habilite essa propriedade em producao.</li>
 * </ol>
 */
@Service
@Slf4j
public class EmailService {

    static final String URL_RESEND_PADRAO = "https://api.resend.com/emails";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;
    private final boolean logarCodigo;
    private final String resendApiKey;
    private final String resendUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Autowired
    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.remetente:no-reply@trinots.app}") String remetente,
                        @Value("${app.mail.log-codigo:false}") boolean logarCodigo,
                        @Value("${app.mail.resend.api-key:}") String resendApiKey,
                        @Value("${app.mail.resend.url:" + URL_RESEND_PADRAO + "}") String resendUrl) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.logarCodigo = logarCodigo;
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
        this.resendUrl = resendUrl;
    }

    /** Construtor anterior (somente SMTP/log), mantido por compatibilidade. */
    public EmailService(ObjectProvider<JavaMailSender> mailSender, String remetente, boolean logarCodigo) {
        this(mailSender, remetente, logarCodigo, "", URL_RESEND_PADRAO);
    }

    public void enviarCodigo(Usuario usuario, TipoCodigoEnum tipo, String codigo, int validadeMinutos) {
        String assunto;
        String texto;
        if (tipo == TipoCodigoEnum.VERIFICACAO_EMAIL) {
            assunto = "Trinots - Confirme seu e-mail";
            texto = "Olá, " + usuario.getNome() + "!\n\n"
                    + "Seu código para confirmar o e-mail é: " + codigo + "\n\n"
                    + "Ele vale por " + validadeMinutos + " minutos. "
                    + "Se você não criou uma conta no Trinots, ignore esta mensagem.";
        } else {
            assunto = "Trinots - Código para redefinir sua senha";
            texto = "Olá, " + usuario.getNome() + "!\n\n"
                    + "Seu código para redefinir a senha é: " + codigo + "\n\n"
                    + "Ele vale por " + validadeMinutos + " minutos. "
                    + "Se você não pediu a redefinição, ignore esta mensagem.";
        }

        if (!resendApiKey.isEmpty()) {
            enviarPorApiHttp(usuario, tipo, assunto, texto);
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Envio de e-mail não configurado (sem spring.mail.host nem app.mail.resend.api-key). Código {} não foi enviado.", tipo);
            if (logarCodigo) {
                log.info("[DESENVOLVIMENTO] Código {} para {}: {}", tipo, usuario.getEmail(), codigo);
            }
            return;
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(usuario.getEmail());
            mensagem.setSubject(assunto);
            mensagem.setText(texto);
            sender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de {} para o usuário {}", tipo, usuario.getIdUsuario(), e);
        }
    }

    private void enviarPorApiHttp(Usuario usuario, TipoCodigoEnum tipo, String assunto, String texto) {
        String json = "{\"from\":" + json(remetente)
                + ",\"to\":[" + json(usuario.getEmail()) + "]"
                + ",\"subject\":" + json(assunto)
                + ",\"text\":" + json(texto) + "}";
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(resendUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() / 100 != 2) {
                log.error("Falha ao enviar e-mail de {} pela API (HTTP {}) para o usuário {}",
                        tipo, resposta.statusCode(), usuario.getIdUsuario());
            }
        } catch (IOException | IllegalArgumentException e) {
            log.error("Falha ao enviar e-mail de {} pela API para o usuário {}", tipo, usuario.getIdUsuario(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Envio de e-mail de {} interrompido para o usuário {}", tipo, usuario.getIdUsuario());
        }
    }

    /** Valor como string JSON (com aspas), escapando caracteres especiais. */
    private static String json(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
