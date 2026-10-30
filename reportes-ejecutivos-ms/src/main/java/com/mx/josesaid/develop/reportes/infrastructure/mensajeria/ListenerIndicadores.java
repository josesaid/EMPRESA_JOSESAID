package com.mx.josesaid.develop.reportes.infrastructure.mensajeria;

import com.mx.josesaid.develop.reportes.application.ReportesService;
import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.Periodo;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Recibe indicadores de las áreas por Kafka. Un mensaje inválido o que viola una regla
 * (por ejemplo, periodo cerrado) se registra y se descarta; no se reintenta indefinidamente.
 */
@Component
public class ListenerIndicadores {

    private static final Logger log = LoggerFactory.getLogger(ListenerIndicadores.class);

    private final ReportesService servicio;
    private final ObjectMapper mapper;

    public ListenerIndicadores(ReportesService servicio, ObjectMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${reportes.indicadores.topico:indicadores-areas}", groupId = "reportes-ejecutivos-ms")
    public void recibir(String mensaje) {
        try {
            IndicadorEvento e = mapper.readValue(mensaje, IndicadorEvento.class);
            servicio.registrarIndicador(Area.valueOf(e.area()), e.nombre(), Periodo.de(e.periodo()),
                    e.valor(), e.fuenteActualizadaUtc());
        } catch (RuntimeException e) {
            log.warn("Indicador descartado: {}", e.getMessage());
        }
    }

    /** Formato del mensaje que publica cada área. */
    public record IndicadorEvento(String area, String nombre, String periodo, BigDecimal valor,
                                  Instant fuenteActualizadaUtc) {
    }
}
