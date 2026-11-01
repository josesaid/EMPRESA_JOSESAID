package com.mx.josesaid.develop.nexus.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
    static final String RUTA_CLIENTES_BASE = "/clientes-ms/v1";

    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    private static final Set<String> FUENTES_PROSPECTO = Set.of("CAMPANA", "REFERIDO", "EVENTO");
    private static final Set<String> MOTIVOS_CANCELACION = Set.of("CANCELACION_CLIENTE", "ERROR_CAPTURA");
    private static final Set<String> CANALES = Set.of("WEB", "TIENDA", "TELEFONO");
    private static final Set<String> TASAS_IVA = Set.of("IVA_GENERAL", "IVA_FRONTERIZO", "IVA_EXENTO");
    private static final Set<String> DESTINOS_PROSPECTO = Set.of("CONTACTADO", "CALIFICADO", "PERDIDO");
    /** Mismas transiciones que el dominio de clientes-ms; CONVERTIDO no se alcanza por cambio de estatus. */
    private static final Map<String, Set<String>> TRANSICIONES_PROSPECTO = Map.of(
            "NUEVO", Set.of("CONTACTADO", "PERDIDO"),
            "CONTACTADO", Set.of("CALIFICADO", "PERDIDO"),
            "CALIFICADO", Set.of("PERDIDO"));
    private static final Set<String> ROLES_CONTACTO = Set.of("COMPRAS", "PAGOS", "ASISTENCIA");
    private static final Set<String> TIPOS_INTERACCION = Set.of("LLAMADA", "CORREO", "VISITA");

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

    @Tool(name = "enviar_cotizacion",
          description = "Prepara el envío de una cotización en borrador. NO la envía: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación.")
    public String enviarCotizacion(
            @ToolParam(description = "Identificador UUID de la cotización en borrador", required = true) String cotizacionId) {
        RegistroHerramientasUsadas.registrar("enviar_cotizacion");
        return prepararAccionCotizacion(cotizacionId, "BORRADOR", AccionesPendientes.TIPO_COTIZACION_ENVIAR,
                "Enviar cotización", "Envío preparado, no ejecutado.", "solo una cotización en borrador puede enviarse");
    }

    @Tool(name = "aceptar_cotizacion",
          description = "Prepara la aceptación de una cotización enviada. NO la acepta: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación.")
    public String aceptarCotizacion(
            @ToolParam(description = "Identificador UUID de la cotización enviada", required = true) String cotizacionId) {
        RegistroHerramientasUsadas.registrar("aceptar_cotizacion");
        return prepararAccionCotizacion(cotizacionId, "ENVIADA", AccionesPendientes.TIPO_COTIZACION_ACEPTAR,
                "Aceptar cotización", "Aceptación preparada, no ejecutada.", "solo una cotización enviada puede aceptarse");
    }

    @Tool(name = "convertir_cotizacion_en_pedido",
          description = "Prepara la conversión de una cotización aceptada en pedido. NO crea el pedido: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación.")
    public String convertirCotizacionEnPedido(
            @ToolParam(description = "Identificador UUID de la cotización aceptada", required = true) String cotizacionId) {
        RegistroHerramientasUsadas.registrar("convertir_cotizacion_en_pedido");
        return prepararAccionCotizacion(cotizacionId, "ACEPTADA", AccionesPendientes.TIPO_COTIZACION_A_PEDIDO,
                "Convertir cotización en pedido", "Conversión preparada, no ejecutada.",
                "solo una cotización aceptada puede convertirse en pedido");
    }

    @Tool(name = "crear_cotizacion_borrador",
          description = "Prepara una cotización nueva para un cliente con una línea. NO la guarda: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarla en la aplicación. Úsala solo cuando el usuario pida crear una cotización.")
    public String crearCotizacionBorrador(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String clienteId,
            @ToolParam(description = "Canal de venta: WEB, TIENDA o TELEFONO", required = true) String canal,
            @ToolParam(description = "Moneda ISO, por ejemplo MXN", required = true) String moneda,
            @ToolParam(description = "Tasa de IVA: IVA_GENERAL, IVA_FRONTERIZO o IVA_EXENTO", required = true) String tasaIva,
            @ToolParam(description = "Fecha hasta la que vale la cotización, en formato AAAA-MM-DD", required = true) String vigenteHasta,
            @ToolParam(description = "Identificador UUID del producto", required = true) String productoId,
            @ToolParam(description = "Descripción de la línea. Puede ir vacía.", required = false) String descripcion,
            @ToolParam(description = "Cantidad entera mayor a cero", required = true) int cantidad,
            @ToolParam(description = "Precio unitario en la moneda de la cotización", required = true) String precioUnitario) {
        RegistroHerramientasUsadas.registrar("crear_cotizacion_borrador");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(clienteId);
        if (id == null) {
            return "El identificador del cliente no es válido.";
        }
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor a cero.";
        }
        String canalNormalizado = normalizar(canal);
        if (!CANALES.contains(canalNormalizado)) {
            return "El canal debe ser WEB, TIENDA o TELEFONO.";
        }
        String tasaNormalizada = normalizar(tasaIva);
        if (!TASAS_IVA.contains(tasaNormalizada)) {
            return "La tasa de IVA debe ser IVA_GENERAL, IVA_FRONTERIZO o IVA_EXENTO.";
        }
        LocalDate vigencia;
        try {
            vigencia = LocalDate.parse(vigenteHasta.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            return "La vigencia debe ir en formato AAAA-MM-DD, por ejemplo 2026-12-31.";
        }
        if (cliente(id) == null) {
            return "No encontré ese cliente.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("clienteId", id.toString());
        cuerpo.put("canal", canalNormalizado);
        cuerpo.put("moneda", moneda.trim().toUpperCase());
        cuerpo.put("tasaIva", tasaNormalizada);
        cuerpo.put("vigenteHasta", vigencia.toString());
        cuerpo.put("lineas", List.of(linea(productoId, descripcion, cantidad, precioUnitario)));
        String resumen = "Cotización para el cliente " + id + " vigente hasta " + vigencia + ": " + cantidad + " × "
                + productoId.trim() + " a " + precioUnitario.trim() + " " + moneda.trim().toUpperCase()
                + " (" + canalNormalizado + ", " + tasaNormalizada + ")";
        return guardarAccion(AccionesPendientes.TIPO_COTIZACION_CREAR, aJson(cuerpo), resumen,
                "Cotización preparada, no guardada.");
    }

    @Tool(name = "reemplazar_lineas_pedido",
          description = "Prepara el reemplazo de las líneas de un pedido en borrador por una sola línea. NO lo cambia: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación.")
    public String reemplazarLineasPedido(
            @ToolParam(description = "Identificador UUID del pedido en borrador", required = true) String pedidoId,
            @ToolParam(description = "Identificador UUID del producto", required = true) String productoId,
            @ToolParam(description = "Descripción de la línea. Puede ir vacía.", required = false) String descripcion,
            @ToolParam(description = "Cantidad entera mayor a cero", required = true) int cantidad,
            @ToolParam(description = "Precio unitario en la moneda del pedido", required = true) String precioUnitario) {
        RegistroHerramientasUsadas.registrar("reemplazar_lineas_pedido");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(pedidoId);
        if (id == null) {
            return "El identificador del pedido no es válido.";
        }
        if (cantidad <= 0) {
            return "La cantidad debe ser mayor a cero.";
        }
        JsonNode pedido = pedido(id);
        if (pedido == null) {
            return "No encontré ese pedido.";
        }
        String estatus = pedido.path("estatus").asText();
        if (!"BORRADOR".equals(estatus)) {
            return "El pedido está en estatus " + estatus + "; solo un pedido en borrador admite cambios en sus líneas.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pedidoId", id.toString());
        cuerpo.put("lineas", List.of(linea(productoId, descripcion, cantidad, precioUnitario)));
        String resumen = "Reemplazar las líneas del pedido " + id + " por " + cantidad + " × " + productoId.trim()
                + " a " + precioUnitario.trim() + " (estatus actual BORRADOR)";
        return guardarAccion(AccionesPendientes.TIPO_PEDIDO_REEMPLAZAR_LINEAS, aJson(cuerpo), resumen,
                "Cambio de líneas preparado, no ejecutado.");
    }

    @Tool(name = "registrar_devolucion_pedido",
          description = "Prepara la devolución total de un pedido entregado. NO la registra: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación.")
    public String registrarDevolucionPedido(
            @ToolParam(description = "Identificador UUID del pedido entregado", required = true) String pedidoId) {
        RegistroHerramientasUsadas.registrar("registrar_devolucion_pedido");
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
        if (!"ENTREGADO".equals(estatus)) {
            return "El pedido está en estatus " + estatus + "; solo un pedido entregado puede devolverse.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("pedidoId", id.toString());
        String resumen = "Devolución total del pedido " + id + " (estatus actual ENTREGADO)";
        return guardarAccion(AccionesPendientes.TIPO_PEDIDO_DEVOLVER, aJson(cuerpo), resumen,
                "Devolución preparada, no registrada.");
    }

    @Tool(name = "cambiar_estatus_prospecto",
          description = "Prepara el cambio de estatus de un prospecto a CONTACTADO, CALIFICADO o PERDIDO. NO lo cambia: devuelve un resumen y un accion_id. "
                  + "Para convertir un prospecto en cliente usa convertir_prospecto_en_cliente. El usuario debe confirmarlo en la aplicación.")
    public String cambiarEstatusProspecto(
            @ToolParam(description = "Identificador UUID del prospecto", required = true) String prospectoId,
            @ToolParam(description = "Nuevo estatus: CONTACTADO, CALIFICADO o PERDIDO", required = true) String estatus) {
        RegistroHerramientasUsadas.registrar("cambiar_estatus_prospecto");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(prospectoId);
        if (id == null) {
            return "El identificador del prospecto no es válido.";
        }
        String destino = normalizar(estatus);
        if (!DESTINOS_PROSPECTO.contains(destino)) {
            return "El estatus debe ser CONTACTADO, CALIFICADO o PERDIDO. Para convertir un prospecto usa convertir_prospecto_en_cliente.";
        }
        JsonNode prospecto = prospecto(id);
        if (prospecto == null) {
            return "No encontré ese prospecto.";
        }
        String actual = prospecto.path("estatus").asText();
        if (!TRANSICIONES_PROSPECTO.getOrDefault(actual, Set.of()).contains(destino)) {
            return "No se puede pasar el prospecto de " + actual + " a " + destino + ".";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("prospectoId", id.toString());
        cuerpo.put("estatus", destino);
        String resumen = "Cambiar el estatus del prospecto " + id + " de " + actual + " a " + destino;
        return guardarAccion(AccionesPendientes.TIPO_PROSPECTO_ESTATUS, aJson(cuerpo), resumen,
                "Cambio de estatus preparado, no ejecutado.");
    }

    @Tool(name = "convertir_prospecto_en_cliente",
          description = "Prepara la conversión de un prospecto calificado en cliente con su contacto principal. "
                  + "NO lo convierte: devuelve un resumen y un accion_id. El usuario debe confirmarlo en la aplicación.")
    public String convertirProspectoEnCliente(
            @ToolParam(description = "Identificador UUID del prospecto calificado", required = true) String prospectoId,
            @ToolParam(description = "Razón social del cliente", required = true) String razonSocial,
            @ToolParam(description = "RFC del cliente", required = true) String rfc,
            @ToolParam(description = "Nombre del contacto principal", required = true) String contactoNombre,
            @ToolParam(description = "Correo electrónico del contacto principal", required = true) String contactoCorreo,
            @ToolParam(description = "Rol del contacto: COMPRAS, PAGOS o ASISTENCIA", required = true) String contactoRol) {
        RegistroHerramientasUsadas.registrar("convertir_prospecto_en_cliente");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(prospectoId);
        if (id == null) {
            return "El identificador del prospecto no es válido.";
        }
        if (vacio(razonSocial) || vacio(rfc) || vacio(contactoNombre) || vacio(contactoCorreo)
                || !contactoCorreo.contains("@")) {
            return "Indica la razón social, el RFC y un contacto con nombre y correo válido.";
        }
        String rol = normalizar(contactoRol);
        if (!ROLES_CONTACTO.contains(rol)) {
            return "El rol del contacto debe ser COMPRAS, PAGOS o ASISTENCIA.";
        }
        JsonNode prospecto = prospecto(id);
        if (prospecto == null) {
            return "No encontré ese prospecto.";
        }
        String estatus = prospecto.path("estatus").asText();
        if (!"CALIFICADO".equals(estatus)) {
            return "El prospecto está en estatus " + estatus + "; solo un prospecto calificado puede convertirse en cliente.";
        }
        Map<String, Object> contacto = new LinkedHashMap<>();
        contacto.put("nombre", contactoNombre.trim());
        contacto.put("correo", contactoCorreo.trim());
        contacto.put("rol", rol);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("prospectoId", id.toString());
        cuerpo.put("razonSocial", razonSocial.trim());
        cuerpo.put("rfc", rfc.trim().toUpperCase());
        cuerpo.put("contactoPrincipal", contacto);
        String resumen = "Convertir el prospecto " + id + " en el cliente " + razonSocial.trim()
                + " con contacto " + contactoNombre.trim() + " (" + rol + ")";
        return guardarAccion(AccionesPendientes.TIPO_PROSPECTO_CONVERTIR, aJson(cuerpo), resumen,
                "Conversión preparada, no ejecutada.");
    }

    @Tool(name = "registrar_interaccion_cliente",
          description = "Prepara el registro de una interacción con un cliente: llamada, correo o visita. NO la guarda: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarla en la aplicación.")
    public String registrarInteraccionCliente(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String clienteId,
            @ToolParam(description = "Tipo de interacción: LLAMADA, CORREO o VISITA", required = true) String tipo,
            @ToolParam(description = "Fecha y hora ISO 8601 con zona, por ejemplo 2026-11-01T10:00:00-06:00", required = true) String fecha,
            @ToolParam(description = "Resumen de lo tratado, máximo 2000 caracteres", required = true) String resumen) {
        RegistroHerramientasUsadas.registrar("registrar_interaccion_cliente");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(clienteId);
        if (id == null) {
            return "El identificador del cliente no es válido.";
        }
        String tipoNormalizado = normalizar(tipo);
        if (!TIPOS_INTERACCION.contains(tipoNormalizado)) {
            return "El tipo debe ser LLAMADA, CORREO o VISITA.";
        }
        Instant momento;
        try {
            momento = OffsetDateTime.parse(fecha.trim()).toInstant();
        } catch (DateTimeParseException | NullPointerException e) {
            return "La fecha debe ir en formato ISO 8601 con zona, por ejemplo 2026-11-01T10:00:00-06:00.";
        }
        if (vacio(resumen) || resumen.trim().length() > 2000) {
            return "El resumen es obligatorio y no puede pasar de 2000 caracteres.";
        }
        if (cliente(id) == null) {
            return "No encontré ese cliente.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("clienteId", id.toString());
        cuerpo.put("tipo", tipoNormalizado);
        cuerpo.put("fecha", momento.toString());
        cuerpo.put("resumen", resumen.trim());
        String encabezado = "Registrar " + tipoNormalizado + " con el cliente " + id + " el " + momento;
        return guardarAccion(AccionesPendientes.TIPO_INTERACCION_REGISTRAR, aJson(cuerpo), encabezado,
                "Interacción preparada, no guardada.");
    }

    private static Map<String, Object> linea(String productoId, String descripcion, int cantidad, String precioUnitario) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("productoId", productoId.trim());
        linea.put("descripcion", descripcion == null || descripcion.isBlank() ? "Artículo" : descripcion.trim());
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", precioUnitario.trim());
        return linea;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String prepararAccionCotizacion(String cotizacionId, String estatusRequerido, String tipo,
                                            String etiqueta, String encabezado, String motivoRechazo) {
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(cotizacionId);
        if (id == null) {
            return "El identificador de la cotización no es válido.";
        }
        JsonNode cotizacion = cotizacion(id);
        if (cotizacion == null) {
            return "No encontré esa cotización.";
        }
        String estatus = cotizacion.path("estatus").asText();
        if (!estatusRequerido.equals(estatus)) {
            return "La cotización está en estatus " + estatus + "; " + motivoRechazo + ".";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("cotizacionId", id.toString());
        String resumen = etiqueta + " " + id + " (estatus actual " + estatus + ")";
        return guardarAccion(tipo, aJson(cuerpo), resumen, encabezado);
    }

    /** Consulta la cotización con el token del usuario. Devuelve null si no existe o no es visible para él. */
    private JsonNode cotizacion(UUID id) {
        return recurso(RUTA_BASE + "/cotizaciones/" + id);
    }

    /** Consulta el pedido con el token del usuario. Devuelve null si no existe o no es visible para él. */
    private JsonNode pedido(UUID id) {
        return recurso(RUTA_BASE + "/pedidos/" + id);
    }

    /** Consulta el prospecto con el token del usuario. Devuelve null si no existe o no es visible para él. */
    private JsonNode prospecto(UUID id) {
        return recurso(RUTA_CLIENTES_BASE + "/prospectos/" + id);
    }

    /** Consulta el cliente con el token del usuario. Devuelve null si no existe o no es visible para él. */
    private JsonNode cliente(UUID id) {
        return recurso(RUTA_CLIENTES_BASE + "/clientes/" + id);
    }

    private JsonNode recurso(String ruta) {
        try {
            String cuerpo = gateway.get()
                    .uri(ruta)
                    .retrieve()
                    .body(String.class);
            return JSON.readTree(cuerpo);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta no válida de " + ruta, e);
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
