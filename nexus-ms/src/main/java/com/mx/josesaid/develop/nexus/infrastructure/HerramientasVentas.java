package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    private static final Set<String> FUENTES_PROSPECTO = Set.of("CAMPANA", "REFERIDO", "EVENTO");
    private static final Set<String> MOTIVOS_CANCELACION = Set.of("CANCELACION_CLIENTE", "ERROR_CAPTURA");

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

    @Tool(name = "crear_prospecto_borrador",
          description = "Prepara un prospecto nuevo. NO lo guarda: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Úsala solo cuando el usuario pida registrar un prospecto.")
    public String crearProspectoBorrador(
            @ToolParam(description = "Nombre del prospecto", required = true) String nombre,
            @ToolParam(description = "Correo electrónico del prospecto", required = true) String correo,
            @ToolParam(description = "RFC del prospecto. Vacío si no se conoce.", required = false) String rfc,
            @ToolParam(description = "Fuente del prospecto: CAMPANA, REFERIDO o EVENTO", required = true) String fuente) {
        RegistroHerramientasUsadas.registrar("crear_prospecto_borrador");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        String fuenteNormalizada = fuente == null ? "" : fuente.trim().toUpperCase();
        if (!FUENTES_PROSPECTO.contains(fuenteNormalizada)) {
            return "La fuente debe ser CAMPANA, REFERIDO o EVENTO.";
        }
        if (nombre == null || nombre.isBlank() || correo == null || !correo.contains("@")) {
            return "Indica el nombre y un correo válido del prospecto.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombre", nombre.trim());
        cuerpo.put("correo", correo.trim());
        if (rfc != null && !rfc.isBlank()) {
            cuerpo.put("rfc", rfc.trim().toUpperCase());
        }
        cuerpo.put("fuente", fuenteNormalizada);
        String resumen = "Prospecto nuevo: " + nombre.trim() + " (" + correo.trim() + "), fuente " + fuenteNormalizada;
        return guardarAccion(AccionesPendientes.TIPO_PROSPECTO_BORRADOR, aJson(cuerpo), resumen,
                "Prospecto preparado, no guardado.");
    }

    @Tool(name = "confirmar_pedido",
          description = "Prepara la confirmación de un pedido en borrador. NO lo confirma: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación.")
    public String confirmarPedido(
            @ToolParam(description = "Identificador UUID del pedido en borrador", required = true) String pedidoId) {
        RegistroHerramientasUsadas.registrar("confirmar_pedido");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(pedidoId);
        if (id == null) {
            return "El identificador del pedido no es válido.";
        }
        JsonNode pedido = pedido(id);
        if (pedido == null) {
            return "No encontré ese pedido.";
        }
        String estatus = pedido.path("estatus").asText();
        if (!"BORRADOR".equals(estatus)) {
            return "El pedido está en estatus " + estatus + "; solo un pedido en borrador puede confirmarse.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pedidoId", id.toString());
        String resumen = "Confirmar pedido " + id + " (estatus actual BORRADOR)";
        return guardarAccion(AccionesPendientes.TIPO_PEDIDO_CONFIRMAR, aJson(cuerpo), resumen,
                "Confirmación preparada, no ejecutada.");
    }

    @Tool(name = "cancelar_pedido",
          description = "Prepara la cancelación de un pedido en borrador o confirmado. NO lo cancela: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Los motivos válidos son CANCELACION_CLIENTE o ERROR_CAPTURA.")
    public String cancelarPedido(
            @ToolParam(description = "Identificador UUID del pedido", required = true) String pedidoId,
            @ToolParam(description = "Motivo: CANCELACION_CLIENTE o ERROR_CAPTURA", required = true) String motivo) {
        RegistroHerramientasUsadas.registrar("cancelar_pedido");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(pedidoId);
        if (id == null) {
            return "El identificador del pedido no es válido.";
        }
        String motivoNormalizado = motivo == null ? "" : motivo.trim().toUpperCase();
        if (!MOTIVOS_CANCELACION.contains(motivoNormalizado)) {
            return "El motivo debe ser CANCELACION_CLIENTE o ERROR_CAPTURA.";
        }
        JsonNode pedido = pedido(id);
        if (pedido == null) {
            return "No encontré ese pedido.";
        }
        String estatus = pedido.path("estatus").asText();
        if (!"BORRADOR".equals(estatus) && !"CONFIRMADO".equals(estatus)) {
            return "El pedido está en estatus " + estatus + "; solo un pedido en borrador o confirmado puede cancelarse.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pedidoId", id.toString());
        cuerpo.put("motivo", motivoNormalizado);
        String resumen = "Cancelar pedido " + id + " por " + motivoNormalizado + " (estatus actual " + estatus + ")";
        return guardarAccion(AccionesPendientes.TIPO_PEDIDO_CANCELAR, aJson(cuerpo), resumen,
                "Cancelación preparada, no ejecutada.");
    }

    private String guardarAccion(String tipo, String payload, String resumen, String encabezado) {
        AccionPendiente accion = acciones.preparar(usuarioActual(), tipo, payload, resumen);
        return FiltroDatosSensibles.redactar(encabezado + " " + resumen
                + ". accion_id: " + accion.id() + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.")
                .texto();
    }

    /** Consulta el pedido con el token del usuario. Devuelve null si no existe o no es visible para él. */
    private JsonNode pedido(UUID id) {
        try {
            String cuerpo = gateway.get()
                    .uri(RUTA_BASE + "/pedidos/" + id)
                    .retrieve()
                    .body(String.class);
            return JSON.readTree(cuerpo);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta de pedidos no válida", e);
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
