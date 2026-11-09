package com.mx.josesaid.develop.nexus.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas sobre facturacion-pagos-ms, por gateway-ms con el token del usuario.
 * Las de lectura consultan datos; la de escritura solo prepara una acción que el usuario confirma en la aplicación.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasFinanzas {

    static final String RUTA_BASE = "/facturacion-pagos-ms/v1";
    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    public HerramientasFinanzas(RestClient gateway) {
        this(gateway, null);
    }

    public HerramientasFinanzas(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
    }

    @Tool(name = "consultar_comprobante",
          description = "Devuelve un comprobante por su identificador UUID: folio, subtotal, IVA, total, estatus, "
                  + "estatus de cobro, monto pagado, saldo y lista de pagos.")
    public String consultarComprobante(
            @ToolParam(description = "Identificador UUID del comprobante", required = true) String comprobanteId) {
        RegistroHerramientasUsadas.registrar("consultar_comprobante");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/comprobantes/{id}")
                        .build(UUID.fromString(comprobanteId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "buscar_comprobante_por_folio",
          description = "Devuelve un comprobante por su folio exacto (por ejemplo, el folio asignado al emitir).")
    public String buscarComprobantePorFolio(
            @ToolParam(description = "Folio exacto del comprobante", required = true) String folio) {
        RegistroHerramientasUsadas.registrar("buscar_comprobante_por_folio");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/comprobantes")
                        .queryParam("folio", folio.trim())
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "listar_pagos_proveedor",
          description = "Lista los últimos 15 pagos registrados a proveedores, del más reciente al más antiguo: "
                  + "monto, fecha de pago, referencia y la cuenta por pagar a la que pertenecen.")
    public String listarPagosProveedor() {
        RegistroHerramientasUsadas.registrar("listar_pagos_proveedor");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/cuentas-por-pagar/pagos")
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "listar_comprobantes_pendientes_cobro",
          description = "Lista los últimos 15 comprobantes emitidos con saldo por cobrar, del más reciente al más "
                  + "antiguo: folio, moneda, total, saldo y estatus de cobro (PENDIENTE o PARCIAL).")
    public String listarComprobantesPendientesCobro() {
        RegistroHerramientasUsadas.registrar("listar_comprobantes_pendientes_cobro");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/comprobantes/pendientes-cobro")
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "consultar_cuenta_por_pagar",
          description = "Devuelve una cuenta por pagar a proveedor por su identificador UUID: proveedor, monto, saldo y pagos.")
    public String consultarCuentaPorPagar(
            @ToolParam(description = "Identificador UUID de la cuenta por pagar", required = true) String cuentaId) {
        RegistroHerramientasUsadas.registrar("consultar_cuenta_por_pagar");
        UUID id = identificador(cuentaId);
        if (id == null) {
            return "El identificador de la cuenta por pagar no es válido.";
        }
        try {
            String cuerpo = gateway.get()
                    .uri(RUTA_BASE + "/cuentas-por-pagar/{id}", id)
                    .retrieve()
                    .body(String.class);
            return FiltroDatosSensibles.redactar(cuerpo).texto();
        } catch (HttpClientErrorException.NotFound e) {
            return "No encontré esa cuenta por pagar.";
        }
    }

    @Tool(name = "registrar_pago_comprobante",
          description = "Prepara el registro de un cobro sobre un comprobante. NO lo registra: devuelve un resumen y un "
                  + "accion_id. El usuario debe confirmarlo en la aplicación. Solo FINANZAS puede usarla.")
    public String registrarPagoComprobante(
            @ToolParam(description = "Identificador UUID del comprobante", required = true) String comprobanteId,
            @ToolParam(description = "Monto mayor a cero, hasta 2 decimales", required = true) String monto,
            @ToolParam(description = "Fecha del pago en formato AAAA-MM-DD; vacío para hoy", required = false) String fechaPago,
            @ToolParam(description = "Referencia bancaria o de transferencia, de hasta 100 caracteres", required = false) String referencia) {
        RegistroHerramientasUsadas.registrar("registrar_pago_comprobante");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(comprobanteId);
        if (id == null) {
            return "El identificador del comprobante no es válido.";
        }
        BigDecimal cantidad = decimal(monto);
        if (cantidad == null || cantidad.signum() <= 0) {
            return "El monto debe ser mayor a cero, con hasta 2 decimales.";
        }
        LocalDate fecha = null;
        if (fechaPago != null && !fechaPago.isBlank()) {
            try {
                fecha = LocalDate.parse(fechaPago.trim());
            } catch (DateTimeParseException e) {
                return "La fecha debe tener formato AAAA-MM-DD.";
            }
        }
        if (referencia != null && referencia.trim().length() > 100) {
            return "La referencia no puede tener más de 100 caracteres.";
        }
        try {
            gateway.get().uri(RUTA_BASE + "/comprobantes/{id}", id).retrieve().body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return "No encontré ese comprobante.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("comprobanteId", id.toString());
        cuerpo.put("monto", cantidad);
        if (fecha != null) {
            cuerpo.put("fechaPago", fecha.toString());
        }
        if (referencia != null && !referencia.isBlank()) {
            cuerpo.put("referencia", referencia.trim());
        }
        // Clave única por acción: si el cobro se reintenta, facturación no lo aplica dos veces.
        cuerpo.put("idempotencyKey", java.util.UUID.randomUUID().toString());
        String resumen = "Registrar un cobro de " + cantidad.toPlainString() + " sobre el comprobante " + id
                + (fecha == null ? "" : " con fecha " + fecha);
        AccionPendiente accion = acciones.preparar(usuarioActual(), AccionesPendientes.TIPO_COMPROBANTE_PAGO_REGISTRAR,
                aJson(cuerpo), resumen);
        return FiltroDatosSensibles.redactar("Cobro preparado, no registrado. " + resumen + ". accion_id: "
                + accion.id() + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.").texto();
    }

    /** Número decimal de hasta 2 decimales y 12 dígitos enteros; null si no es válido. */
    private static BigDecimal decimal(String valor) {
        try {
            BigDecimal numero = new BigDecimal(valor.trim());
            if (numero.scale() > 2 || numero.precision() - numero.scale() > 12) {
                return null;
            }
            return numero;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    private static UUID identificador(String valor) {
        try {
            return valor == null ? null : UUID.fromString(valor.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static String aJson(Map<String, Object> datos) {
        try {
            return JSON.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la acción", e);
        }
    }
}
