package com.mx.josesaid.develop.nexus.comun.filtro;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacta datos sensibles de un texto antes de que salga hacia el modelo.
 * Tipos: RFC, correo, teléfono (10 dígitos MX), cuenta CLABE (18 dígitos) y tarjeta (16 dígitos).
 */
public final class FiltroDatosSensibles {

    /** Tipos de dato sensible. Cada uno tiene la marca que reemplaza al dato en el texto. */
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

        /** Marca con la que se reemplaza el dato, por ejemplo [CORREO]. */
        public String marca() {
            return marca;
        }
    }

    /**
     * Texto ya redactado y cuántas veces se redactó cada tipo.
     *
     * @param texto componente de tipo {@code String}.
     * @param conteo componente de tipo {@code Map<Tipo, Integer>}.
     */
    public record Resultado(String texto, Map<Tipo, Integer> conteo) {
        public Resultado {
            conteo = Map.copyOf(conteo);
        }

        /** Número total de datos redactados, sumando todos los tipos. */
        public int total() {
            return conteo.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    // Persona física (13) y moral (12): 3-4 letras, fecha AAMMDD, 3 caracteres homoclave.
    private static final Pattern RFC = Pattern.compile("\\b[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}\\b");
    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    // 10 dígitos, opcionalmente con prefijo 52 o +52 y separadores; no se pega a montos con coma o punto.
    // No puede estar pegado a letras, dígitos ni guiones: así no corta UUID ni identificadores como 6a78ec71-9742-4122-...
    /** Teléfono de 10 dígitos con o sin separadores. Se describe en el comentario de arriba. */
    private static final Pattern TELEFONO = Pattern.compile(
            "(?<![\\p{L}\\p{N}_-])(?<!\\d[.,])(?:\\+?52[\\s-]?)?(?:\\d{2}[\\s-]?\\d{4}[\\s-]?\\d{4}|\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{4})(?![\\p{L}\\p{N}_-])(?![.,]\\d)");
    private static final Pattern CLABE = Pattern.compile("(?<!\\d)(?<!\\d[.,])\\d{18}(?!\\d)(?![.,]\\d)");
    private static final Pattern TARJETA = Pattern.compile("(?<!\\d)(?<!\\d[.,])(?:\\d{4}[\\s-]?){3}\\d{4}(?!\\d)(?![.,]\\d)");

    /** Clase de utilidad: no se instancia. */
    private FiltroDatosSensibles() {
    }

    /** Redacta todos los tipos de dato sensible. El orden importa: primero CLABE y tarjeta, que son números largos, para que el teléfono no los corte. */
    public static Resultado redactar(String texto) {
        return redactar(texto, EnumSet.noneOf(Tipo.class));
    }

    /** Igual que {@link #redactar(String)}, pero deja intactos los tipos indicados en {@code omitir}. */
    public static Resultado redactar(String texto, Set<Tipo> omitir) {
        Map<Tipo, Integer> conteo = new EnumMap<>(Tipo.class);
        if (texto == null || texto.isEmpty()) {
            return new Resultado(texto, conteo);
        }
        String salida = texto;
        // Orden: primero los más específicos para que no se mezclen entre sí.
        if (!omitir.contains(Tipo.CLABE)) salida = reemplazar(salida, CLABE, Tipo.CLABE, conteo);
        if (!omitir.contains(Tipo.TARJETA)) salida = reemplazar(salida, TARJETA, Tipo.TARJETA, conteo);
        if (!omitir.contains(Tipo.CORREO)) salida = reemplazar(salida, CORREO, Tipo.CORREO, conteo);
        if (!omitir.contains(Tipo.RFC)) salida = reemplazar(salida, RFC, Tipo.RFC, conteo);
        if (!omitir.contains(Tipo.TELEFONO)) salida = reemplazar(salida, TELEFONO, Tipo.TELEFONO, conteo);
        return new Resultado(salida, conteo);
    }

    /** Reemplaza todas las coincidencias de un patrón por su marca y suma las cantidades en el conteo. */
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
