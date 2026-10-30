package com.mx.josesaid.develop.nexus.comun.filtro;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacta datos sensibles de un texto antes de que salga hacia el modelo.
 * Tipos: RFC, correo, teléfono (10 dígitos MX), cuenta CLABE (18 dígitos) y tarjeta (16 dígitos).
 */
public final class FiltroDatosSensibles {

    public enum Tipo {
        RFC("[RFC]"),
        CORREO("[CORREO]"),
        TELEFONO("[TELEFONO]"),
        CLABE("[CUENTA]"),
        TARJETA("[TARJETA]");

        private final String marca;

        Tipo(String marca) {
            this.marca = marca;
        }

        public String marca() {
            return marca;
        }
    }

    public record Resultado(String texto, Map<Tipo, Integer> conteo) {
        public int total() {
            return conteo.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    // Persona física (13) y moral (12): 3-4 letras, fecha AAMMDD, 3 caracteres homoclave.
    private static final Pattern RFC = Pattern.compile("\\b[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}\\b");
    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    // 10 dígitos, opcionalmente con prefijo 52 o +52 y separadores; no se pega a montos con coma o punto.
    private static final Pattern TELEFONO = Pattern.compile(
            "(?<!\\d)(?<!\\d[.,])(?:\\+?52[\\s-]?)?(?:\\d{2}[\\s-]?\\d{4}[\\s-]?\\d{4}|\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4})(?!\\d)(?![.,]\\d)");
    private static final Pattern CLABE = Pattern.compile("(?<!\\d)(?<!\\d[.,])\\d{18}(?!\\d)(?![.,]\\d)");
    private static final Pattern TARJETA = Pattern.compile("(?<!\\d)(?<!\\d[.,])(?:\\d{4}[\\s-]?){3}\\d{4}(?!\\d)(?![.,]\\d)");

    private FiltroDatosSensibles() {
    }

    public static Resultado redactar(String texto) {
        Map<Tipo, Integer> conteo = new EnumMap<>(Tipo.class);
        if (texto == null || texto.isEmpty()) {
            return new Resultado(texto, conteo);
        }
        String salida = texto;
        // Orden: primero los más específicos para que no se mezclen entre sí.
        salida = reemplazar(salida, CLABE, Tipo.CLABE, conteo);
        salida = reemplazar(salida, TARJETA, Tipo.TARJETA, conteo);
        salida = reemplazar(salida, CORREO, Tipo.CORREO, conteo);
        salida = reemplazar(salida, RFC, Tipo.RFC, conteo);
        salida = reemplazar(salida, TELEFONO, Tipo.TELEFONO, conteo);
        return new Resultado(salida, conteo);
    }

    private static String reemplazar(String texto, Pattern patron, Tipo tipo, Map<Tipo, Integer> conteo) {
        Matcher m = patron.matcher(texto);
        int n = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            n++;
            m.appendReplacement(sb, Matcher.quoteReplacement(tipo.marca()));
        }
        m.appendTail(sb);
        if (n > 0) {
            conteo.merge(tipo, n, Integer::sum);
        }
        return sb.toString();
    }
}
