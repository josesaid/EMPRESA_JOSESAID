package com.mx.josesaid.develop.inventario.infrastructure.pedidos;

import com.mx.josesaid.develop.inventario.application.ReservasPedidoService;
import com.mx.josesaid.develop.inventario.application.ReservasPedidoService.LineaPedido;
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
 * Aplica a inventario los eventos de pedidos-ventas-ms: reserva al confirmar y descuenta al entregar.
 * Un mensaje mal formado se registra y se descarta. Un fallo de base de datos se propaga para que Kafka reintente.
 */
@Component
public class ListenerPedidos {

    private static final Logger log = LoggerFactory.getLogger(ListenerPedidos.class);

    private final ReservasPedidoService servicio;
    private final ObjectMapper mapper;
    private final String almacenConfigurado;

    /** Recibe el servicio de reservas, el lector JSON y la configuración del tópico de pedidos. */
    public ListenerPedidos(ReservasPedidoService servicio, ObjectMapper mapper,
                           @Value("${inventario.pedidos.almacen-id:}") String almacenConfigurado) {
        this.servicio = servicio;
        this.mapper = mapper;
        this.almacenConfigurado = almacenConfigurado;
    }

    /** Recibe desde Kafka los eventos de pedidos: confirmado reserva existencias y entregado las descuenta. */
    @KafkaListener(topics = "${inventario.pedidos.topico:pedidos.eventos}",
                   groupId = "inventario-operaciones-ms-pedidos")
    public void recibir(String mensaje) {
        Sobre sobre;
        try {
            sobre = mapper.readValue(mensaje, Sobre.class);
        } catch (RuntimeException e) {
            log.warn("Evento de pedidos descartado: {}", e.getClass().getSimpleName());
            return;
        }
        String tipo = sobre.tipo();
        if (!ReservasPedidoService.EVENTO_CONFIRMADO.equals(tipo) && !ReservasPedidoService.EVENTO_ENTREGADO.equals(tipo)) {
            return;
        }
        UUID pedidoId;
        try {
            pedidoId = UUID.fromString(sobre.agregadoId());
        } catch (RuntimeException e) {
            log.warn("Evento {} descartado por identificador inválido", tipo);
            return;
        }
        if (ReservasPedidoService.EVENTO_ENTREGADO.equals(tipo)) {
            servicio.entregar(pedidoId);
            return;
        }
        List<LineaPedido> lineas;
        try {
            lineas = aLineas(sobre.datos());
        } catch (RuntimeException e) {
            log.warn("Evento {} descartado por datos inválidos: {}", tipo, e.getClass().getSimpleName());
            return;
        }
        if (almacenConfigurado == null || almacenConfigurado.isBlank()) {
            log.error("Pedido {}: falta inventario.pedidos.almacen-id; no se reserva", pedidoId);
            return;
        }
        servicio.reservar(pedidoId, UUID.fromString(almacenConfigurado), lineas);
    }

    /** Convierte las líneas del evento en líneas de pedido. */
    private static List<LineaPedido> aLineas(Map<String, Object> datos) {
        Object crudas = datos == null ? null : datos.get("lineas");
        if (!(crudas instanceof List<?> lista)) {
            throw new IllegalArgumentException("El evento no trae líneas");
        }
        return lista.stream()
                .map(m -> (Map<?, ?>) m)
                .map(m -> new LineaPedido(UUID.fromString(String.valueOf(m.get("productoId"))),
                        new BigDecimal(String.valueOf(m.get("cantidad")))))
                .toList();
    }

    /** Sobre que publica pedidos-ventas-ms por el outbox. */
    public record Sobre(String tipo, String agregadoId, Map<String, Object> datos) {
    }
}
