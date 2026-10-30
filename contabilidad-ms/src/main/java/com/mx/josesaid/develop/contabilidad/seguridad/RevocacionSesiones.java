package com.mx.josesaid.develop.contabilidad.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Sesiones cerradas, recordadas en memoria solo mientras pueda haber tokens vigentes.
 * Cada servicio mantiene su copia: un reinicio la vacía, y los tokens revocados expiran igual en 15 minutos.
 */
@Component
public class RevocacionSesiones {

    /** Vida del access token (15 min) más un margen. Pasado ese tiempo, ningún token revocado sigue vigente. */
    public static final Duration VIDA = Duration.ofMinutes(20);

    private final Map<String, Instant> sesiones = new ConcurrentHashMap<>();
    private final Map<String, Instant> usuarios = new ConcurrentHashMap<>();

    /** Revoca una sesión (su sid) cerrada en el instante dado. */
    public void revocarSesion(String sid, Instant ocurridoEn) {
        if (sid == null || vencido(ocurridoEn)) {
            return;
        }
        sesiones.put(sid, ocurridoEn);
    }

    /** Revoca los tokens del usuario emitidos antes del instante dado. */
    public void revocarUsuario(String usuarioId, Instant ocurridoEn) {
        if (usuarioId == null || vencido(ocurridoEn)) {
            return;
        }
        usuarios.merge(usuarioId, ocurridoEn, (actual, nuevoValor) -> nuevoValor.isAfter(actual) ? nuevoValor : actual);
    }

    /** true si un token con ese sid, usuario y fecha de emisión ya no debe aceptarse. */
    public boolean revocado(String sid, String usuarioId, Instant emitidoEn) {
        limpiar();
        if (sid != null && sesiones.containsKey(sid)) {
            return true;
        }
        Instant cerradoEn = usuarioId == null ? null : usuarios.get(usuarioId);
        return cerradoEn != null && emitidoEn != null && emitidoEn.isBefore(cerradoEn);
    }

    private static boolean vencido(Instant ocurridoEn) {
        return ocurridoEn == null || ocurridoEn.plus(VIDA).isBefore(Instant.now());
    }

    private void limpiar() {
        Instant limite = Instant.now().minus(VIDA);
        sesiones.values().removeIf(t -> t.isBefore(limite));
        usuarios.values().removeIf(t -> t.isBefore(limite));
    }
}
