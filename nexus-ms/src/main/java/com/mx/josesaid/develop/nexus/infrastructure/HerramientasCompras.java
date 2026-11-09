package com.mx.josesaid.develop.nexus.infrastructure;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas sobre compras-proveedores-ms, por gateway-ms con el token del usuario.
 * Las de lectura consultan datos; las de escritura solo preparan una acción que el usuario confirma en la aplicación.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasCompras {

    static final String RUTA_BASE = "/compras-proveedores-ms/v1";
    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    public HerramientasCompras(RestClient gateway) {
        this(gateway, null);
    }

    public HerramientasCompras(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
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

    @Tool(name = "crear_proveedor",
          description = "Prepara el alta de un proveedor. NO lo crea: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Solo COMPRAS puede usarla.")
    public String crearProveedor(
            @ToolParam(description = "Razón social del proveedor, de hasta 200 caracteres", required = true) String razonSocial,
            @ToolParam(description = "RFC del proveedor, de hasta 13 caracteres", required = true) String rfc,
            @ToolParam(description = "Correo electrónico del proveedor", required = true) String correo) {
        RegistroHerramientasUsadas.registrar("crear_proveedor");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        if (vacio(razonSocial) || razonSocial.trim().length() > 200) {
            return "La razón social es obligatoria y de hasta 200 caracteres.";
        }
        String rfcLimpio = rfc == null ? "" : rfc.trim().toUpperCase();
        if (rfcLimpio.isEmpty() || rfcLimpio.length() > 13) {
            return "El RFC es obligatorio y de hasta 13 caracteres.";
        }
        if (!correoValido(correo)) {
            return "El correo del proveedor no es válido.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("razonSocial", razonSocial.trim());
        cuerpo.put("rfc", rfcLimpio);
        cuerpo.put("correo", correo.trim());
        String resumen = "Alta de proveedor " + razonSocial.trim() + " (RFC " + rfcLimpio + ")";
        return guardar(AccionesPendientes.TIPO_PROVEEDOR_CREAR, cuerpo, resumen, "Alta preparada, no creada.");
    }

    @Tool(name = "crear_orden_compra",
          description = "Prepara una orden de compra en borrador para un proveedor, con una sola línea. "
                  + "NO la crea: devuelve un resumen y un accion_id. El usuario debe confirmarlo en la aplicación. "
                  + "Solo COMPRAS puede usarla.")
    public String crearOrdenCompra(
            @ToolParam(description = "Identificador UUID del proveedor", required = true) String proveedorId,
            @ToolParam(description = "Moneda ISO, por ejemplo MXN", required = true) String moneda,
            @ToolParam(description = "SKU del artículo", required = true) String articuloSku,
            @ToolParam(description = "Cantidad mayor a cero, hasta 3 decimales", required = true) String cantidad,
            @ToolParam(description = "Precio unitario, hasta 2 decimales", required = true) String precioUnitario) {
        RegistroHerramientasUsadas.registrar("crear_orden_compra");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(proveedorId);
        if (id == null) {
            return "El identificador del proveedor no es válido.";
        }
        String monedaLimpia = normalizar(moneda);
        if (monedaLimpia.isEmpty() || monedaLimpia.length() > 3) {
            return "La moneda debe ser un código ISO de hasta 3 letras, por ejemplo MXN.";
        }
        if (vacio(articuloSku) || articuloSku.trim().length() > 40) {
            return "El SKU es obligatorio y de hasta 40 caracteres.";
        }
        BigDecimal cant = decimal(cantidad, 3, 9);
        if (cant == null || cant.signum() <= 0) {
            return "La cantidad debe ser mayor a cero, con hasta 3 decimales y 9 dígitos enteros.";
        }
        BigDecimal precio = decimal(precioUnitario, 2, 10);
        if (precio == null || precio.signum() < 0) {
            return "El precio unitario no es válido: debe ser cero o positivo, con hasta 2 decimales.";
        }
        if (recurso(RUTA_BASE + "/proveedores/" + id) == null) {
            return "No encontré ese proveedor.";
        }
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("articuloSku", articuloSku.trim());
        linea.put("cantidad", cant);
        linea.put("precioUnitario", precio);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("proveedorId", id.toString());
        cuerpo.put("moneda", monedaLimpia);
        cuerpo.put("lineas", java.util.List.of(linea));
        String resumen = "Orden de compra al proveedor " + id + ": " + cant.stripTrailingZeros().toPlainString()
                + " x " + articuloSku.trim() + " a " + precio.toPlainString() + " " + monedaLimpia;
        return guardar(AccionesPendientes.TIPO_ORDEN_COMPRA_CREAR, cuerpo, resumen, "Orden preparada, no creada.");
    }

    @Tool(name = "registrar_recepcion_orden",
          description = "Prepara el registro de una recepción de mercancía en una orden aprobada o recibida parcialmente. "
                  + "NO la registra: devuelve un resumen y un accion_id. El usuario debe confirmarlo en la aplicación. "
                  + "Solo ALMACEN puede usarla.")
    public String registrarRecepcionOrden(
            @ToolParam(description = "Identificador UUID de la orden de compra", required = true) String ordenId,
            @ToolParam(description = "SKU del artículo recibido", required = true) String articuloSku,
            @ToolParam(description = "Cantidad recibida, mayor a cero", required = true) String cantidad) {
        RegistroHerramientasUsadas.registrar("registrar_recepcion_orden");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(ordenId);
        if (id == null) {
            return "El identificador de la orden no es válido.";
        }
        if (vacio(articuloSku) || articuloSku.trim().length() > 40) {
            return "El SKU es obligatorio y de hasta 40 caracteres.";
        }
        BigDecimal cant = decimal(cantidad, 3, 9);
        if (cant == null || cant.signum() <= 0) {
            return "La cantidad debe ser mayor a cero, con hasta 3 decimales y 9 dígitos enteros.";
        }
        JsonNode orden = recurso(RUTA_BASE + "/ordenes-compra/" + id);
        if (orden == null) {
            return "No encontré esa orden.";
        }
        String estatus = orden.path("estatus").asText();
        if (!"APROBADA".equals(estatus) && !"RECIBIDA_PARCIAL".equals(estatus)) {
            return "La orden está en estatus " + estatus + "; solo una orden aprobada o recibida parcialmente admite recepciones.";
        }
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("articuloSku", articuloSku.trim());
        linea.put("cantidad", cant);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ordenId", id.toString());
        cuerpo.put("lineas", java.util.List.of(linea));
        String resumen = "Recepción de " + cant.stripTrailingZeros().toPlainString() + " x " + articuloSku.trim()
                + " en la orden " + id + " (estatus actual " + estatus + ")";
        return guardar(AccionesPendientes.TIPO_ORDEN_COMPRA_RECEPCION, cuerpo, resumen, "Recepción preparada, no registrada.");
    }

    @Tool(name = "cancelar_orden_compra",
          description = "Prepara la cancelación de una orden de compra que no esté recibida ni cancelada. "
                  + "NO la cancela: devuelve un resumen y un accion_id. El usuario debe confirmarlo en la aplicación. "
                  + "Solo COMPRAS puede usarla.")
    public String cancelarOrdenCompra(
            @ToolParam(description = "Identificador UUID de la orden de compra", required = true) String ordenId,
            @ToolParam(description = "Motivo de la cancelación, de hasta 200 caracteres", required = true) String motivo) {
        RegistroHerramientasUsadas.registrar("cancelar_orden_compra");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(ordenId);
        if (id == null) {
            return "El identificador de la orden no es válido.";
        }
        if (vacio(motivo) || motivo.trim().length() > 200) {
            return "El motivo es obligatorio y de hasta 200 caracteres.";
        }
        JsonNode orden = recurso(RUTA_BASE + "/ordenes-compra/" + id);
        if (orden == null) {
            return "No encontré esa orden.";
        }
        String estatus = orden.path("estatus").asText();
        if ("RECIBIDA".equals(estatus) || "CANCELADA".equals(estatus)) {
            return "La orden está en estatus " + estatus + "; no puede cancelarse.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ordenId", id.toString());
        cuerpo.put("motivo", motivo.trim());
        String resumen = "Cancelar la orden " + id + " (estatus actual " + estatus + "): " + motivo.trim();
        return guardar(AccionesPendientes.TIPO_ORDEN_COMPRA_CANCELAR, cuerpo, resumen, "Cancelación preparada, no ejecutada.");
    }

    private String guardar(String tipo, Map<String, Object> cuerpo, String resumen, String encabezado) {
        AccionPendiente accion = acciones.preparar(usuarioActual(), tipo, aJson(cuerpo), resumen);
        return FiltroDatosSensibles.redactar(encabezado + " " + resumen + ". accion_id: " + accion.id()
                + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.").texto();
    }

    /** Consulta un recurso con el token del usuario. Devuelve null si no existe o no es visible para él. */
    private JsonNode recurso(String ruta) {
        try {
            String cuerpo = gateway.get().uri(ruta).retrieve().body(String.class);
            return JSON.readTree(cuerpo);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta no válida de " + ruta, e);
        }
    }

    /** Número decimal con la escala y los dígitos enteros máximos; null si no es válido. */
    private static BigDecimal decimal(String valor, int escala, int enteros) {
        try {
            BigDecimal numero = new BigDecimal(valor.trim());
            if (numero.scale() > escala || numero.precision() - numero.scale() > enteros) {
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

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static boolean correoValido(String correo) {
        return !vacio(correo) && correo.trim().length() <= 254 && correo.contains("@");
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(java.util.Locale.ROOT);
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
