package com.mx.josesaid.develop.facturacion_pagos.infrastructure.compras;

import com.mx.josesaid.develop.facturacion_pagos.application.CuentasPorPagarService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Crea una cuenta por pagar por cada orden aprobada en compras-proveedores-ms.
 * Un mensaje mal formado o con datos inválidos se registra y se descarta. Un fallo de base de datos se propaga
 * para que Kafka reintente.
 */
@Component
public class ListenerOrdenesCompra {

    private static final Logger log = LoggerFactory.getLogger(ListenerOrdenesCompra.class);
    static final String TIPO_ORDEN_APROBADA = "orden-compra-aprobada";

    private final CuentasPorPagarService servicio;
    private final ObjectMapper mapper;

    /** Recibe el servicio de cuentas por pagar y el lector JSON. */
    public ListenerOrdenesCompra(CuentasPorPagarService servicio, ObjectMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    /** Recibe desde Kafka la orden de compra aprobada y crea su cuenta por pagar. */
    @KafkaListener(topics = "${cuentas.topico-compras:compras.eventos}",
                   groupId = "facturacion-pagos-ms-cuentas-por-pagar",
                   properties = "auto.offset.reset=earliest")
    public void recibir(String mensaje) {
        Datos datos;
        try {
            datos = aDatos(mensaje);
        } catch (RuntimeException e) {
            log.warn("Evento de compras descartado: {}", e.getClass().getSimpleName());
            return;
        }
        if (datos != null) {
            servicio.registrarCuenta(datos.ordenId(), datos.proveedorId(), datos.moneda(), datos.total());
        }
    }

    /** Devuelve null para eventos ajenos; lanza IllegalArgumentException para datos inválidos. */
    private Datos aDatos(String mensaje) {
        Sobre sobre = mapper.readValue(mensaje, Sobre.class);
        if (!TIPO_ORDEN_APROBADA.equals(sobre.tipo())) {
            return null;
        }
        Map<String, Object> d = sobre.datos();
        String moneda = String.valueOf(d.get("moneda"));
        if (!"MXN".equals(moneda) && !"USD".equals(moneda)) {
            throw new IllegalArgumentException("moneda inválida o ausente en la orden " + d.get("id"));
        }
        BigDecimal total = new BigDecimal(String.valueOf(d.get("total")));
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("total no positivo en la orden " + d.get("id"));
        }
        return new Datos(UUID.fromString(String.valueOf(d.get("id"))),
                UUID.fromString(String.valueOf(d.get("proveedor_id"))), moneda, total);
    }

    /**
     * Datos extraídos del evento: orden, proveedor, moneda y total.
     *
     * @param ordenId componente de tipo {@code UUID}.
     * @param proveedorId componente de tipo {@code UUID}.
     * @param moneda componente de tipo {@code String}.
     * @param total componente de tipo {@code BigDecimal}.
     */
    private record Datos(UUID ordenId, UUID proveedorId, String moneda, BigDecimal total) {
    }

    /**
     * Sobre que publica compras-proveedores-ms por el outbox.
     *
     * @param tipo componente de tipo {@code String}.
     * @param agregadoId componente de tipo {@code String}.
     * @param datos componente de tipo {@code Map<String, Object>}.
     */
    public record Sobre(String tipo, String agregadoId, Map<String, Object> datos) {
    }
}
