package com.mx.josesaid.develop.documentos.infrastructure.indicadores;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
 * Publica a reportes-ejecutivos-ms el indicador "documentos por vencer en 30 días" del área legal.
 *
 * <p>Cuenta los documentos no eliminados que vencen entre hoy y dentro de 30 días. Depende de la fecha
 * de hoy, así que solo publica el mes actual. Es una foto, no un evento: no pasa por el outbox; si un
 * envío falla, el siguiente ciclo lo corrige.
 */
@Component
public class PublicadorIndicadoresVencimientos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorIndicadoresVencimientos.class);

    /** Valor del enum Area de reportes-ejecutivos-ms. */
    public static final String AREA = "ADMINISTRACION_LEGAL";
    public static final String NOMBRE = "documentos_por_vencer_30d";

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private static final String SQL_VENCIMIENTOS = """
            SELECT COUNT(*)::numeric
            FROM documentos
            WHERE eliminado = FALSE
              AND vence_en BETWEEN ? AND ?
            """;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final Clock reloj;
    private final String topico;

    public PublicadorIndicadoresVencimientos(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
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
        LocalDate hoy = LocalDate.now(reloj.withZone(ZONA));
        BigDecimal valor = jdbc.queryForObject(SQL_VENCIMIENTOS, BigDecimal.class,
                java.sql.Date.valueOf(hoy), java.sql.Date.valueOf(hoy.plusDays(30)));
        String periodo = YearMonth.from(hoy).toString();
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
