package com.mx.josesaid.develop.compras.infrastructure.indicadores;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica a reportes-ejecutivos-ms dos indicadores mensuales del área de compras:
 * <ul>
 *   <li>{@code ordenes_mes}: órdenes creadas en el mes, en cualquier estatus (incluidas canceladas).</li>
 *   <li>{@code ordenes_canceladas_mes}: de esas, las que están en estatus CANCELADA.</li>
 * </ul>
 * Las órdenes anteriores a V3 reciben, en la migración V4, la fecha en que se aplicó esa migración.
 * Se recalculan el mes actual y el anterior en cada ciclo. Son fotos del periodo, no eventos: no pasan
 * por el outbox; si un envío falla, el siguiente ciclo lo corrige.
 */
@Component
public class PublicadorIndicadoresOrdenes {

    private static final Logger log = LoggerFactory.getLogger(PublicadorIndicadoresOrdenes.class);

    /** Valor del enum Area de reportes-ejecutivos-ms. */
    public static final String AREA = "COMPRAS_PROVEEDORES";
    public static final String NOMBRE = "ordenes_mes";
    public static final String NOMBRE_CANCELADAS = "ordenes_canceladas_mes";

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private static final String SQL_ORDENES = """
            SELECT COUNT(*)::numeric
            FROM ordenes_compra
            WHERE creado_utc IS NOT NULL
              AND TO_CHAR(creado_utc AT TIME ZONE 'America/Mexico_City', 'YYYY-MM') = ?
            """;

    private static final String SQL_CANCELADAS = """
            SELECT COUNT(*)::numeric
            FROM ordenes_compra
            WHERE creado_utc IS NOT NULL
              AND estatus = 'CANCELADA'
              AND TO_CHAR(creado_utc AT TIME ZONE 'America/Mexico_City', 'YYYY-MM') = ?
            """;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final Clock reloj;
    private final String topico;

    public PublicadorIndicadoresOrdenes(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
                                        ObjectMapper mapper, Clock reloj,
                                        @Value("${indicadores.topico:indicadores-areas}") String topico) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.mapper = mapper;
        this.reloj = reloj;
        this.topico = topico;
    }

    @Scheduled(fixedDelayString = "${indicadores.intervalo-ms:3600000}",
            initialDelayString = "${indicadores.inicio-ms:30000}")
    public void publicar() {
        YearMonth actual = YearMonth.now(reloj.withZone(ZONA));
        publicarPeriodo(actual.minusMonths(1).toString());
        publicarPeriodo(actual.toString());
    }

    /** Calcula y envía los dos indicadores de un mes ("AAAA-MM"). */
    public void publicarPeriodo(String periodo) {
        enviar(NOMBRE, periodo, jdbc.queryForObject(SQL_ORDENES, BigDecimal.class, periodo));
        enviar(NOMBRE_CANCELADAS, periodo, jdbc.queryForObject(SQL_CANCELADAS, BigDecimal.class, periodo));
    }

    /** Un fallo se registra y se reintenta en el siguiente ciclo. */
    private void enviar(String nombre, String periodo, BigDecimal valor) {
        String payload = mapper.writeValueAsString(
                new IndicadorEvento(AREA, nombre, periodo, valor, Instant.now(reloj)));
        try {
            kafka.send(topico, AREA + "|" + nombre + "|" + periodo, payload).get(10, TimeUnit.SECONDS);
            log.info("Indicador publicado: {} {} {} = {}", AREA, nombre, periodo, valor);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("No se pudo publicar el indicador {} {}: {}", nombre, periodo, ex.getMessage());
        }
    }

    /** Formato que espera reportes-ejecutivos-ms (ListenerIndicadores.IndicadorEvento). */
    public record IndicadorEvento(String area, String nombre, String periodo, BigDecimal valor,
                                  Instant fuenteActualizadaUtc) {
    }
}
