package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;

/**
 * Índices de búsqueda y de duplicados: HMAC-SHA256 en hexadecimal (64 caracteres).
 * Cada valor se normaliza antes de firmarse y lleva un prefijo por tipo para que dos tipos
 * de dato distintos nunca produzcan el mismo índice.
 */
public final class IndiceHash {

    private IndiceHash() {
    }

    /** RFC en mayúsculas, sin espacios. */
    public static String rfc(String valor) {
        return hmac("rfc:" + valor.trim().toUpperCase(Locale.ROOT));
    }

    /** Correo en minúsculas, sin espacios. */
    public static String correo(String valor) {
        return hmac("correo:" + valor.trim().toLowerCase(Locale.ROOT));
    }

    /** Nombre sin acentos, en minúsculas y con espacios simples. */
    public static String nombre(String valor) {
        return hmac("nombre:" + normalizarNombre(valor));
    }

    static String normalizarNombre(String valor) {
        String sinAcentos = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String hmac(String entrada) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(ClavesCripto.indice());
            return HexFormat.of().formatHex(mac.doFinal(entrada.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el índice de búsqueda", e);
        }
    }
}
