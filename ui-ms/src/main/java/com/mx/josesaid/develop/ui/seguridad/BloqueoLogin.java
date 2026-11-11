package com.mx.josesaid.develop.ui.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Bloquea temporalmente el login de un correo tras varios fallos seguidos. Vive en memoria de la app. */
@Component
public class BloqueoLogin {

    static final int MAX_FALLOS = 5;
    static final Duration DURACION = Duration.ofMinutes(5);

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    /** Indica si el correo está bloqueado por demasiados intentos fallidos. */
    public boolean bloqueado(String correo) {
        String clave = clave(correo);
        Estado estado = estados.get(clave);
        if (estado == null || estado.hasta == null) {
            return false;
        }
        if (Instant.now().isBefore(estado.hasta)) {
            return true;
        }
        estados.remove(clave, estado);
        return false;
    }

    /** Registra un intento fallido. Al llegar al máximo, bloquea el correo por 5 minutos. */
    public void fallo(String correo) {
        estados.compute(clave(correo), (clave, previo) -> {
            Estado estado = previo == null ? new Estado() : previo;
            estado.fallos++;
            if (estado.fallos >= MAX_FALLOS) {
                estado.hasta = Instant.now().plus(DURACION);
                estado.fallos = 0;
            }
            return estado;
        });
    }

    /** Limpia los fallos del correo tras un inicio de sesión exitoso. */
    public void exito(String correo) {
        estados.remove(clave(correo));
    }

    /** Normaliza el correo (sin espacios, en minúsculas) para usarlo como clave. */
    private static String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Estado {
        int fallos;
        Instant hasta;
    }
}
