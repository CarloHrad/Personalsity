package com.example.trinots.validacao;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suporte comum aos testes de validacao de DTOs (equivalente a "mandar o JSON no Postman" e ver o 400).
 * Usa o Bean Validation direto, sem subir o Spring, entao e rapido e deterministico.
 */
abstract class ValidacaoSuporte {

    private static final ValidatorFactory FABRICA = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
    private static final Validator VALIDATOR = FABRICA.getValidator();

    /** campo -> mensagens (juntas por " | "). */
    protected static Map<String, String> erros(Object dto) {
        Set<ConstraintViolation<Object>> violacoes = VALIDATOR.validate(dto);
        Map<String, String> mapa = new TreeMap<>();
        for (ConstraintViolation<Object> v : violacoes) {
            mapa.merge(v.getPropertyPath().toString(), v.getMessage(), (a, b) -> a + " | " + b);
        }
        return mapa;
    }

    protected static void assertValido(Object dto) {
        assertEquals(Map.of(), erros(dto), "O DTO deveria ser valido");
    }

    protected static void assertInvalido(Object dto, String campo) {
        Map<String, String> e = erros(dto);
        assertTrue(e.containsKey(campo), "Esperava erro em '" + campo + "', mas os erros foram: " + e);
    }

    protected static void assertInvalido(Object dto, String campo, String trechoMensagem) {
        Map<String, String> e = erros(dto);
        assertTrue(e.containsKey(campo), "Esperava erro em '" + campo + "', mas os erros foram: " + e);
        assertTrue(e.get(campo).contains(trechoMensagem),
                "Mensagem de '" + campo + "' era '" + e.get(campo) + "', esperava conter '" + trechoMensagem + "'");
    }

    /** Somente o campo indicado deve falhar (garante que um caso nao quebra campos vizinhos). */
    protected static void assertSoEsteCampoInvalido(Object dto, String campo) {
        Map<String, String> e = erros(dto);
        assertEquals(java.util.Set.of(campo), e.keySet(), "Erros: " + e);
    }

    protected static String repete(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    /** Strings de ataque usadas em todos os campos de texto. */
    protected static final class Ataques {
        private Ataques() {}

        /** Contem caracteres que os @Pattern de nome rejeitam (; = ( ) " digitos). */
        public static final String[] SQL_COM_SIMBOLOS = {
                "' OR '1'='1",
                "'; DROP TABLE usuario;--",
                "Robert'); DROP TABLE Students;--",
                "1; SELECT * FROM usuario",
                "\" OR \"\"=\"",
                "admin'/**/OR/**/1=1",
        };

        /** So letras/espacos/aspas/hifen: PASSAM no @Pattern de nome de pessoa; a defesa e a consulta parametrizada. */
        public static final String[] SQL_SO_LETRAS = {
                "x' AND SLEEP(5)--",
                "admin'--",
                "' UNION SELECT senha FROM usuario--",
                "Maria' OR 'a' OR '",
        };

        public static final String[] XSS = {
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>",
                "\"><svg/onload=alert(1)>",
        };

        /** ':' e '(' sao permitidos no nome de tarefa, entao este passa la (e e so texto). */
        public static final String XSS_URI = "javascript:alert(1)";

        public static final String[] CONTROLE = {
                "Jo\u0000ao",
                "Jo\nao",
                "Jo\tao",
                "Jo\rao",
        };

        public static final String[] INJECAO_FORMULA_CSV = {
                "=cmd|' /C calc'!A0",
                "+1+1",
                "@SUM(A1)",
                "-2+3",
        };
    }
}
