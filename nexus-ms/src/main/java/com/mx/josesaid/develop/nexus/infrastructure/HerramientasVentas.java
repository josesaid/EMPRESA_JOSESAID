package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre pedidos-ventas-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasVentas {

    static final String RUTA_BASE = "/pedidos-ventas-ms/v1";

    private final RestClient gateway;

    public HerramientasVentas(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "consultar_pedidos",
          description = "Lista pedidos de venta. Filtros opcionales: estatus (BORRADOR, CONFIRMADO, ENTREGADO, CANCELADO), "
                  + "canal (WEB, TIENDA, TELEFONO) y clienteId (UUID). Vacío para no filtrar.")
    public String consultarPedidos(
            @ToolParam(description = "Estatus del pedido: BORRADOR, CONFIRMADO, ENTREGADO o CANCELADO. Vacío para todos.",
                       required = false) String estatus,
            @ToolParam(description = "Canal de venta: WEB, TIENDA o TELEFONO. Vacío para todos.", required = false) String canal,
            @ToolParam(description = "Identificador UUID del cliente. Vacío para todos.", required = false) String clienteId) {
        RegistroHerramientasUsadas.registrar("consultar_pedidos");
        String cuerpo = gateway.get()
                .uri(constructor -> {
                    var uri = constructor.path(RUTA_BASE + "/pedidos");
                    if (estatus != null && !estatus.isBlank()) {
                        uri.queryParam("estatus", estatus.trim().toUpperCase());
                    }
                    if (canal != null && !canal.isBlank()) {
                        uri.queryParam("canal", canal.trim().toUpperCase());
                    }
                    if (clienteId != null && !clienteId.isBlank()) {
                        uri.queryParam("clienteId", clienteId.trim());
                    }
                    return uri.build();
                })
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "resumen_ventas",
          description = "Devuelve el resumen de ventas de un mes: número de pedidos y ventas por moneda. El mes es AAAA-MM.")
    public String resumenVentas(
            @ToolParam(description = "Mes en formato AAAA-MM, por ejemplo 2026-09", required = true) String mes) {
        RegistroHerramientasUsadas.registrar("resumen_ventas");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/metricas/ventas")
                        .queryParam("mes", mes.trim())
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
