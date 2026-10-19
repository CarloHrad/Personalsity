package com.example.trinots.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Montador de JSON minimo para os testes. Evita depender da versao do Jackson (2 ou 3) e permite enviar
 * exatamente o texto que um cliente (Postman) enviaria, inclusive JSON proposital quebrado.
 */
final class Json {
    private Json() {}

    /** Json.obj("nome", "Ana", "idade", 3, "curso", Json.obj(...)). Um valor null vira JSON null. */
    static String obj(Object... paresChaveValor) {
        return escrever(mapa(paresChaveValor));
    }

    static Map<String, Object> mapa(Object... paresChaveValor) {
        if (paresChaveValor.length % 2 != 0) throw new IllegalArgumentException("pares chave/valor");
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paresChaveValor.length; i += 2) m.put((String) paresChaveValor[i], paresChaveValor[i + 1]);
        return m;
    }

    /** Copia o mapa trocando (ou incluindo) um campo. Passe REMOVER para tirar a chave do JSON. */
    static Map<String, Object> com(Map<String, Object> base, String campo, Object valor) {
        Map<String, Object> copia = new LinkedHashMap<>(base);
        if (valor == REMOVER) copia.remove(campo); else copia.put(campo, valor);
        return copia;
    }

    static final Object REMOVER = new Object();

    /** Texto JSON ja pronto (inserido sem aspas). */
    record Cru(String texto) {}

    static String escrever(Object v) {
        if (v == null) return "null";
        if (v instanceof Cru c) return c.texto();
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                sb.append(texto(String.valueOf(e.getKey()))).append(':').append(escrever(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(escrever(l.get(i)));
            }
            return sb.append(']').toString();
        }
        return texto(v.toString());
    }

    static String texto(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
