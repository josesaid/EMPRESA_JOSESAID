package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre compras-proveedores-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasCompras {

    static final String RUTA_BASE = "/compras-proveedores-ms/v1";

    private final RestClient gateway;

    public HerramientasCompras(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "consultar_proveedor",
          description = "Devuelve los datos de un proveedor por su identificador UUID: razón social, RFC enmascarado, correo enmascarado y estatus.")
    public String consultarProveedor(
            @ToolParam(description = "Identificador UUID del proveedor", required = true) String proveedorId) {
        RegistroHerramientasUsadas.registrar("consultar_proveedor");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/proveedores/{id}").build(UUID.fromString(proveedorId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "consultar_orden_compra",
          description = "Devuelve una orden de compra por su identificador UUID: proveedor, moneda, líneas, cantidades recibidas, subtotales y estatus.")
    public String consultarOrdenCompra(
            @ToolParam(description = "Identificador UUID de la orden de compra", required = true) String ordenId) {
        RegistroHerramientasUsadas.registrar("consultar_orden_compra");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/ordenes-compra/{id}").build(UUID.fromString(ordenId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "buscar_proveedores",
          description = "Busca proveedores por parte de la razón social (mínimo 3 caracteres). Devuelve hasta 20 resultados con RFC y correo enmascarados.")
    public String buscarProveedores(
            @ToolParam(description = "Texto de la razón social, mínimo 3 caracteres", required = true) String razonSocial) {
        RegistroHerramientasUsadas.registrar("buscar_proveedores");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/proveedores")
                        .queryParam("razonSocial", razonSocial.trim())
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    private static final java.util.Set<String> ESTATUS_ORDEN =
            java.util.Set.of("BORRADOR", "APROBADA", "RECIBIDA_PARCIAL", "RECIBIDA", "CANCELADA");

    @Tool(name = "listar_ordenes_compra",
          description = "Lista las órdenes de compra más recientes primero, hasta 15. Filtro opcional por un solo estatus: BORRADOR, APROBADA, RECIBIDA_PARCIAL, RECIBIDA o CANCELADA. Muestra la razón social del proveedor y la fecha de creación.")
    public String listarOrdenesCompra(
            @ToolParam(description = "Estatus para filtrar, opcional. Vacío para no filtrar.", required = false) String estatus) {
        RegistroHerramientasUsadas.registrar("listar_ordenes_compra");
        String filtro = estatus == null ? "" : estatus.trim().toUpperCase(java.util.Locale.ROOT);
        if (!filtro.isEmpty() && !ESTATUS_ORDEN.contains(filtro)) {
            return "Estatus no válido. Use BORRADOR, APROBADA, RECIBIDA_PARCIAL, RECIBIDA o CANCELADA.";
        }
        String cuerpo = gateway.get()
                .uri(constructor -> {
                    var ruta = constructor.path(RUTA_BASE + "/ordenes-compra");
                    if (!filtro.isEmpty()) {
                        ruta.queryParam("estatus", filtro);
                    }
                    return ruta.build();
                })
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
