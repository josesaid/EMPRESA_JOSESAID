package com.mx.josesaid.develop.nexus.application;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Política de la empresa leída de un archivo Markdown con front matter:
 * título, vigencia (fecha desde la que aplica) y roles que pueden consultarla.
 */
public record Politica(String id, String titulo, LocalDate vigencia, List<String> roles, String cuerpo) {

    private static final String SEPARADOR = "---";

    public Politica {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("La política necesita un identificador");
        }
        // Sin roles nadie podría consultarla: se rechaza al ingerir en lugar de dejarla inaccesible en silencio
        roles = List.copyOf(roles);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("La política " + id + " no tiene roles");
        }
    }

    public static Politica desde(String id, String contenido) {
        String[] lineas = contenido.split("\\R");
        if (lineas.length == 0 || !SEPARADOR.equals(lineas[0].trim())) {
            throw new IllegalArgumentException("La política " + id + " no tiene front matter");
        }
        int fin = -1;
        for (int i = 1; i < lineas.length; i++) {
            if (SEPARADOR.equals(lineas[i].trim())) {
                fin = i;
                break;
            }
        }
        if (fin < 0) {
            throw new IllegalArgumentException("El front matter de " + id + " no cierra");
        }
        Map<String, String> meta = new HashMap<>();
        for (int i = 1; i < fin; i++) {
            String linea = lineas[i];
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos > 0) {
                meta.put(linea.substring(0, dosPuntos).trim(), linea.substring(dosPuntos + 1).trim());
            }
        }
        String titulo = requerido(id, meta, "titulo");
        LocalDate vigencia = LocalDate.parse(requerido(id, meta, "vigencia"));
        List<String> roles = Arrays.stream(requerido(id, meta, "roles").replaceAll("[\\[\\]]", "").split(","))
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .filter(r -> !r.isEmpty())
                .toList();
        String cuerpo = String.join("\n", Arrays.copyOfRange(lineas, fin + 1, lineas.length));
        return new Politica(id, titulo, vigencia, roles, cuerpo);
    }

    private static String requerido(String id, Map<String, String> meta, String clave) {
        String valor = meta.get(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La política " + id + " no define " + clave);
        }
        return valor;
    }
}
