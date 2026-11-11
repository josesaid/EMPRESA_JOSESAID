package com.mx.josesaid.develop.pedidos.infrastructure.idempotencia;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Guarda el estado de cada clave Idempotency-Key. Una respuesta completada se conserva 24 horas.
 * Una reserva que nunca se completó (la petición murió a medio camino) se puede retomar tras
 * {@link #CONCESION_RESERVA}, para que la clave no quede bloqueada todo el día.
 */
@Repository
class RegistroIdempotencia {

    static final Duration VIGENCIA = Duration.ofHours(24);
    static final Duration CONCESION_RESERVA = Duration.ofMinutes(2);

    /** Fila guardada para una clave: huella, estatus HTTP, cuerpo de la respuesta y si ya terminó. */
    record Registro(String huella, int estatus, String cuerpo, boolean completada) {}

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    RegistroIdempotencia(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** Busca el registro de una clave que sigue vigente. */
    Optional<Registro> buscar(String clave) {
        return jdbc.query(
                "SELECT huella, estatus_http, cuerpo, completada FROM idempotencia WHERE clave = ? AND creado_utc > ?",
                (rs, n) -> new Registro(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getBoolean(4)),
                clave, enTiempo(vencimiento())).stream().findFirst();
    }

    /**
     * Reserva la clave. Devuelve false si otra petición la tiene en curso o ya terminó.
     * Si la reserva existente nunca se completó y su concesión venció, la retoma.
     */
    boolean reservar(String clave, String huella) {
        jdbc.update("DELETE FROM idempotencia WHERE creado_utc <= ?", enTiempo(vencimiento()));
        try {
            jdbc.update("INSERT INTO idempotencia (clave, huella, estatus_http, cuerpo, completada, creado_utc) VALUES (?, ?, 0, NULL, FALSE, ?)",
                    clave, huella, enTiempo(reloj.instant()));
            return true;
        } catch (DuplicateKeyException e) {
            return retomarReservaVencida(clave, huella);
        }
    }

    /** Solo una petición gana la retoma: el UPDATE es condicional, así que es atómico. */
    private boolean retomarReservaVencida(String clave, String huella) {
        Instant ahora = reloj.instant();
        int filas = jdbc.update(
                "UPDATE idempotencia SET huella = ?, creado_utc = ? WHERE clave = ? AND completada = FALSE AND creado_utc <= ?",
                huella, enTiempo(ahora), clave, enTiempo(ahora.minus(CONCESION_RESERVA)));
        return filas == 1;
    }

    /** Guarda la respuesta de una operación terminada. */
    void completar(String clave, int estatus, String cuerpo) {
        jdbc.update("UPDATE idempotencia SET estatus_http = ?, cuerpo = ?, completada = TRUE WHERE clave = ?",
                estatus, cuerpo, clave);
    }

    /** Libera la reserva cuando la operación falló, para que el cliente pueda reintentar. */
    void liberar(String clave) {
        jdbc.update("DELETE FROM idempotencia WHERE clave = ? AND completada = FALSE", clave);
    }

    /** Calcula el límite de vigencia de las claves. */
    private Instant vencimiento() {
        return reloj.instant().minus(VIGENCIA);
    }

    /** Convierte un instante a Timestamp para la base de datos. */
    private static Timestamp enTiempo(Instant instante) {
        return Timestamp.from(instante);
    }
}
