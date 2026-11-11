package com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;

/** Índice de búsqueda por correo: HMAC-SHA256 en hexadecimal sobre el correo normalizado. */
public final class IndiceCorreo {

    /** Clase de utilidad; no se instancia. */
    private IndiceCorreo() {
    }

    /** Calcula el índice de búsqueda del correo normalizado, para buscar sin descifrar todos los usuarios. */
    public static String de(String correo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(ClavesCripto.indice());
            byte[] hash = mac.doFinal(("correo:" + correo.trim().toLowerCase(Locale.ROOT))
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el índice de correo", e);
        }
    }
}
