package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre pedidos-ventas-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasVentas {

    static final String RUTA_BASE = "/pedidos-ventas-ms/v1";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    public HerramientasVentas(RestClient gateway) {
        this(gateway, null);
    }

    /** Con acciones nulas la herramienta de preparar pedidos responde que no está disponible. */
    public HerramientasVentas(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
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

    @Tool(name = "preparar_pedido_borrador",
          description = "Prepara un pedido de venta en borrador. NO lo guarda: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación antes de que se guarde. Úsala solo cuando el usuario pida crear un pedido.")
    public String prepararPedidoBorrador(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String clienteId,
            @ToolParam(description = "Canal de venta: WEB, TIENDA o TELEFONO", required = true) String canal,
            @ToolParam(description = "Moneda ISO, por ejemplo MXN", required = true) String moneda,
            @ToolParam(description = "Tasa de IVA: IVA_GENERAL, IVA_FRONTERIZO o IVA_EXENTO", required = true) String tasaIva,
            @ToolParam(description = "Identificador UUID del producto", required = true) String productoId,
            @ToolParam(description = "Descripción de la línea. Puede ir vacía.", required = false) String descripcion,
            @ToolParam(description = "Cantidad entera mayor a cero", required = true) int cantidad,
            @ToolParam(description = "Precio unitario en la moneda del pedido", required = true) String precioUnitario) {
        RegistroHerramientasUsadas.registrar("preparar_pedido_borrador");
        if (acciones == null) {
            return "La preparación de pedidos no está disponible en este momento.";
        }
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor a cero.";
        }
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("productoId", productoId.trim());
        linea.put("descripcion", descripcion == null || descripcion.isBlank() ? "Artículo" : descripcion.trim());
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", precioUnitario.trim());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("clienteId", clienteId.trim());
        cuerpo.put("canal", canal.trim().toUpperCase());
        cuerpo.put("moneda", moneda.trim().toUpperCase());
        cuerpo.put("tasaIva", tasaIva.trim().toUpperCase());
        cuerpo.put("lineas", List.of(linea));
        String payload = aJson(cuerpo);
        String resumen = "Pedido en borrador para el cliente " + clienteId.trim() + ": " + cantidad + " × "
                + productoId.trim() + " a " + precioUnitario.trim() + " " + moneda.trim().toUpperCase()
                + " (" + canal.trim().toUpperCase() + ", " + tasaIva.trim().toUpperCase() + ")";
        AccionPendiente accion = acciones.preparar(usuarioActual(), AccionesPendientes.TIPO_PEDIDO_BORRADOR,
                payload, resumen);
        return FiltroDatosSensibles.redactar("Pedido preparado, no guardado. " + resumen
                + ". accion_id: " + accion.id() + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.")
                .texto();
    }

    private static String usuarioActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getSubject();
        }
        throw new IllegalStateException("No hay un usuario autenticado para preparar la acción");
    }

    private static String aJson(Map<String, Object> cuerpo) {
        try {
            return JSON.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la acción", e);
        }
    }
}
