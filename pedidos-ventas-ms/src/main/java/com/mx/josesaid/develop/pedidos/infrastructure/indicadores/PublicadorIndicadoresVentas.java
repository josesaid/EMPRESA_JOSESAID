package com.mx.josesaid.develop.pedidos.infrastructure.indicadores;

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
 * Publica a reportes-ejecutivos-ms el indicador "ventas del mes" del área de ventas.
 *
 * <p>Ventas = suma de líneas de pedidos en MXN con estatus CONFIRMADO o ENTREGADO, con IVA incluido,
 * asignadas al mes de su confirmación (hora de la Ciudad de México). En cada ciclo se recalculan el mes
 * actual y el anterior. Reportes hace upsert por área, nombre y periodo, así que un valor repetido no
 * duplica nada. Es una foto del periodo, no un evento: no pasa por el outbox. Si un envío falla, el
 * siguiente ciclo lo corrige.
 */
@Component
public class PublicadorIndicadoresVentas {

    private static final Logger log = LoggerFactory.getLogger(PublicadorIndicadoresVentas.class);

    /** Valor del enum Area de reportes-ejecutivos-ms. */
    public static final String AREA = "VENTAS_ATENCION_CLIENTES";
    public static final String NOMBRE = "ventas_mes";

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private static final String SQL_VENTAS = """
            SELECT COALESCE(SUM(ROUND(l.cantidad * l.precio_unitario *
                   (1 + CASE p.tasa WHEN 'IVA_GENERAL' THEN 0.16 WHEN 'IVA_FRONTERIZO' THEN 0.08 ELSE 0 END), 2)), 0)
            FROM pedidos p
            JOIN lineas_pedido l ON l.pedido_id = p.id
            WHERE p.estatus IN ('CONFIRMADO', 'ENTREGADO')
              AND p.moneda = 'MXN'
              AND p.fecha_confirmacion IS NOT NULL
              AND TO_CHAR(p.fecha_confirmacion AT TIME ZONE 'America/Mexico_City', 'YYYY-MM') = ?
            """;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final Clock reloj;
    private final String topico;

    public PublicadorIndicadoresVentas(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
                                       ObjectMapper mapper, Clock reloj,
                                       @Value("${pedidos.indicadores.topico:indicadores-areas}") String topico) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.mapper = mapper;
        this.reloj = reloj;
        this.topico = topico;
    }

    @Scheduled(fixedDelayString = "${pedidos.indicadores.intervalo-ms:3600000}",
            initialDelayString = "${pedidos.indicadores.inicio-ms:30000}")
    public void publicar() {
        YearMonth actual = YearMonth.now(reloj.withZone(ZONA));
        publicarPeriodo(actual.minusMonths(1).toString());
        publicarPeriodo(actual.toString());
    }

    /** Calcula y envía el indicador de un mes ("AAAA-MM"). Un fallo se registra y se reintenta en el siguiente ciclo. */
    public void publicarPeriodo(String periodo) {
        BigDecimal valor = jdbc.queryForObject(SQL_VENTAS, BigDecimal.class, periodo);
        String payload = mapper.writeValueAsString(
                new IndicadorEvento(AREA, NOMBRE, periodo, valor, Instant.now(reloj)));
        try {
            kafka.send(topico, AREA + "|" + NOMBRE + "|" + periodo, payload).get(10, TimeUnit.SECONDS);
            log.info("Indicador publicado: {} {} {} = {}", AREA, NOMBRE, periodo, valor);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("No se pudo publicar el indicador {} {}: {}", NOMBRE, periodo, ex.getMessage());
        }
    }

    /** Formato que espera reportes-ejecutivos-ms (ListenerIndicadores.IndicadorEvento). */
    public record IndicadorEvento(String area, String nombre, String periodo, BigDecimal valor,
                                  Instant fuenteActualizadaUtc) {
    }
}
