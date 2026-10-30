package com.mx.josesaid.develop.facturacion_pagos.infrastructure.idempotencia;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Guarda el estado de cada clave Idempotency-Key durante 24 horas. */
@Repository
class RegistroIdempotencia {

    static final Duration VIGENCIA = Duration.ofHours(24);

    record Registro(String huella, int estatus, String cuerpo, boolean completada) {}

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    RegistroIdempotencia(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    Optional<Registro> buscar(String clave) {
        return jdbc.query(
                "SELECT huella, estatus_http, cuerpo, completada FROM idempotencia WHERE clave = ? AND creado_utc > ?",
                (rs, n) -> new Registro(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getBoolean(4)),
                clave, enTiempo(vencimiento())).stream().findFirst();
    }

    /** Reserva la clave. Devuelve false si ya existe: otra petición la tiene o ya terminó. */
    boolean reservar(String clave, String huella) {
        jdbc.update("DELETE FROM idempotencia WHERE creado_utc <= ?", enTiempo(vencimiento()));
        try {
            jdbc.update("INSERT INTO idempotencia (clave, huella, estatus_http, cuerpo, completada, creado_utc) VALUES (?, ?, 0, NULL, FALSE, ?)",
                    clave, huella, enTiempo(reloj.instant()));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    void completar(String clave, int estatus, String cuerpo) {
        jdbc.update("UPDATE idempotencia SET estatus_http = ?, cuerpo = ?, completada = TRUE WHERE clave = ?",
                estatus, cuerpo, clave);
    }

    /** Libera la reserva cuando la operación falló, para que el cliente pueda reintentar. */
    void liberar(String clave) {
        jdbc.update("DELETE FROM idempotencia WHERE clave = ? AND completada = FALSE", clave);
    }

    private Instant vencimiento() {
        return reloj.instant().minus(VIGENCIA);
    }

    private static Timestamp enTiempo(Instant instante) {
        return Timestamp.from(instante);
    }
}
