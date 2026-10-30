package com.mx.josesaid.develop.contabilidad.infrastructure.indicadores;

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
 * Publica a reportes-ejecutivos-ms el indicador "pólizas del mes" del área de finanzas.
 *
 * <p>Cuenta las pólizas APLICADAS del mes (AAAA-MM). Se recalculan el mes actual y el anterior en cada
 * ciclo. Es una foto del periodo, no un evento: no pasa por el outbox; si un envío falla, el siguiente
 * ciclo lo corrige.
 */
@Component
public class PublicadorIndicadoresPolizas {

    private static final Logger log = LoggerFactory.getLogger(PublicadorIndicadoresPolizas.class);

    /** Valor del enum Area de reportes-ejecutivos-ms. */
    public static final String AREA = "FINANZAS_CONTABILIDAD";
    public static final String NOMBRE = "polizas_mes";

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private static final String SQL_POLIZAS = """
            SELECT COUNT(*)::numeric
            FROM polizas
            WHERE estatus = 'APLICADA'
              AND anio_mes = ?
            """;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final Clock reloj;
    private final String topico;

    public PublicadorIndicadoresPolizas(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
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

    /** Calcula y envía el indicador de un mes ("AAAA-MM"). Un fallo se registra y se reintenta en el siguiente ciclo. */
    public void publicarPeriodo(String periodo) {
        BigDecimal valor = jdbc.queryForObject(SQL_POLIZAS, BigDecimal.class, periodo);
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
