package com.mx.josesaid.develop.inventario.infrastructure.compras;

import com.mx.josesaid.develop.inventario.domain.RecepcionesRepositorio;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class RecepcionesJdbc implements RecepcionesRepositorio {

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    RecepcionesJdbc(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Override
    public boolean marcarProcesada(UUID eventoId) {
        return jdbc.update(
                "INSERT INTO recepciones_procesadas (evento_id, procesado_utc) VALUES (?, ?) ON CONFLICT DO NOTHING",
                eventoId, Timestamp.from(reloj.instant())) == 1;
    }
}
