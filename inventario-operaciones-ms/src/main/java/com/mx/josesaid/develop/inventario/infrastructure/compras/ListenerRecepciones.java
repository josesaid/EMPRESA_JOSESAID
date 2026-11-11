package com.mx.josesaid.develop.inventario.infrastructure.compras;

import com.mx.josesaid.develop.inventario.application.RecepcionesService;
import com.mx.josesaid.develop.inventario.application.RecepcionesService.LineaRecibida;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Aplica a inventario las recepciones de compras-proveedores-ms (evento insumos-recibidos).
 * Un mensaje mal formado o sin evento_id se registra y se descarta. Un fallo de base de datos se propaga para que Kafka reintente.
 */
@Component
public class ListenerRecepciones {

    private static final Logger log = LoggerFactory.getLogger(ListenerRecepciones.class);
    static final String TIPO_RECIBIDOS = "insumos-recibidos";

    private final RecepcionesService servicio;
    private final ObjectMapper mapper;
    private final String almacenConfigurado;

    /** Recibe el servicio de recepciones, el lector JSON y la configuración del tópico de compras. */
    public ListenerRecepciones(RecepcionesService servicio, ObjectMapper mapper,
                               @Value("${inventario.pedidos.almacen-id:}") String almacenConfigurado) {
        this.servicio = servicio;
        this.mapper = mapper;
        this.almacenConfigurado = almacenConfigurado;
    }

    /** Recibe desde Kafka el evento de recepción de una orden de compra y lo registra como entrada de inventario. */
    @KafkaListener(topics = "${inventario.compras.topico:compras.eventos}",
                   groupId = "inventario-operaciones-ms-compras")
    public void recibir(String mensaje) {
        Sobre sobre;
        try {
            sobre = mapper.readValue(mensaje, Sobre.class);
        } catch (RuntimeException e) {
            log.warn("Evento de compras descartado: {}", e.getClass().getSimpleName());
            return;
        }
        if (!TIPO_RECIBIDOS.equals(sobre.tipo())) {
            return;
        }
        UUID eventoId;
        UUID ordenId;
        List<LineaRecibida> lineas;
        try {
            Map<String, Object> datos = sobre.datos();
            eventoId = UUID.fromString(String.valueOf(datos.get("evento_id")));
            ordenId = UUID.fromString(sobre.agregadoId());
            lineas = aLineas(datos);
        } catch (RuntimeException e) {
            log.warn("Recepción descartada por datos inválidos: {}", e.getClass().getSimpleName());
            return;
        }
        if (almacenConfigurado == null || almacenConfigurado.isBlank()) {
            log.error("Orden {}: falta inventario.pedidos.almacen-id; no se registra la recepción", ordenId);
            return;
        }
        servicio.registrar(eventoId, ordenId, UUID.fromString(almacenConfigurado), lineas);
    }

    /** Convierte las líneas del evento en líneas de recepción. */
    private static List<LineaRecibida> aLineas(Map<String, Object> datos) {
        if (!(datos.get("lineas") instanceof List<?> lista)) {
            throw new IllegalArgumentException("La recepción no trae líneas");
        }
        return lista.stream()
                .map(m -> (Map<?, ?>) m)
                .map(m -> new LineaRecibida(String.valueOf(m.get("sku")),
                        new BigDecimal(String.valueOf(m.get("cantidad")))))
                .toList();
    }

    /**
     * Sobre que publica compras-proveedores-ms por el outbox.
     *
     * @param tipo tipo o clasificación del registro, de tipo {@code String}.
     * @param agregadoId identificador del agregado de dominio al que pertenece el evento, de tipo {@code String}.
     * @param datos datos específicos de la operación, de tipo {@code Map<String, Object>}.
     */
    public record Sobre(String tipo, String agregadoId, Map<String, Object> datos) {
    }
}
