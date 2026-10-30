package com.mx.josesaid.develop.usuarios_accesos.infrastructure.indicadores;

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
 * Publica a reportes-ejecutivos-ms el indicador "usuarios activos" del área de administración y legal.
 *
 * <p>Cuenta los usuarios en estatus ACTIVO en este momento. Es una foto del momento, así que solo se
 * publica el mes actual. No pasa por el outbox; si un envío falla, el siguiente ciclo lo corrige.
 */
@Component
public class PublicadorIndicadoresUsuarios {

    private static final Logger log = LoggerFactory.getLogger(PublicadorIndicadoresUsuarios.class);

    /** Valor del enum Area de reportes-ejecutivos-ms. */
    public static final String AREA = "ADMINISTRACION_LEGAL";
    public static final String NOMBRE = "usuarios_activos";

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private static final String SQL_ACTIVOS = """
            SELECT COUNT(*)::numeric
            FROM usuarios
            WHERE estatus = 'ACTIVO'
            """;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final Clock reloj;
    private final String topico;

    public PublicadorIndicadoresUsuarios(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
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
        String periodo = YearMonth.now(reloj.withZone(ZONA)).toString();
        BigDecimal valor = jdbc.queryForObject(SQL_ACTIVOS, BigDecimal.class);
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
