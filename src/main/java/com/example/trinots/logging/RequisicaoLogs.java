package com.example.trinots.logging;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Regras puras (sem servlet) usadas pelo log de acesso: id de requisicao, IP do cliente, nivel do log e
 * formato da linha. Tudo que vem do cliente (path, IP, X-Request-Id) e sanitizado para impedir
 * "log injection" (quebra de linha forjando entradas falsas no log).
 *
 * NUNCA entram no log: corpo da requisicao, query string, header Authorization, senhas, codigos ou e-mails.
 */
public final class RequisicaoLogs {

    public static final String MDC_REQUEST_ID = "requestId";

    private static final Pattern REQUEST_ID_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
    private static final Pattern IP_VALIDO = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    public enum Nivel { INFO, WARN, ERROR }

    private RequisicaoLogs() {}

    /** Troca controles e caracteres nao imprimiveis por "_" e limita o tamanho. Nulo vira "-". */
    public static String sanitizar(String valor, int maximo) {
        if (valor == null || valor.isEmpty()) {
            return "-";
        }
        String s = valor.length() > maximo ? valor.substring(0, maximo) : valor;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isISOControl(c) || Character.isSurrogate(c) || c == ' ' || c == ' ' ? '_' : c);
        }
        return sb.toString();
    }

    /** Devolve o id enviado pelo cliente se for seguro (8 a 64 caracteres [A-Za-z0-9._-]); senao, nulo. */
    public static String requestIdValido(String entrada) {
        return entrada != null && REQUEST_ID_VALIDO.matcher(entrada).matches() ? entrada : null;
    }

    public static String novoRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Atras do proxy do Railway o IP real vem em X-Forwarded-For (primeiro item). O header e controlado
     * pelo cliente, entao so e aceito se tiver formato de IP; do contrario usa o endereco da conexao.
     */
    public static String ipDoCliente(String xForwardedFor, String enderecoRemoto) {
        if (xForwardedFor != null) {
            String primeiro = xForwardedFor.split(",", 2)[0].trim();
            if (IP_VALIDO.matcher(primeiro).matches()) {
                return primeiro;
            }
        }
        return sanitizar(enderecoRemoto, 45);
    }

    /** 5xx = ERROR; 401 e 403 (login invalido, token invalido, acesso negado) = WARN; o resto = INFO. */
    public static Nivel nivelPara(int status) {
        if (status >= 500) return Nivel.ERROR;
        if (status == 401 || status == 403) return Nivel.WARN;
        return Nivel.INFO;
    }

    public static String linha(String metodo, String caminho, int status, long milissegundos, String ip, Object usuarioId) {
        return sanitizar(metodo, 10) + " " + sanitizar(caminho, 200) + " " + status + " " + milissegundos + "ms"
                + " ip=" + sanitizar(ip, 45)
                + " user=" + (usuarioId == null ? "-" : sanitizar(usuarioId.toString(), 40));
    }
}
