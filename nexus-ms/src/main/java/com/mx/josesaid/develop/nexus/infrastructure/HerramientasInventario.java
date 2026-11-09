package com.mx.josesaid.develop.nexus.infrastructure;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas sobre inventario-operaciones-ms, por gateway-ms con el token del usuario.
 * Las de lectura consultan datos; las de escritura solo preparan una acción que el usuario confirma en la aplicación.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasInventario {

    static final String RUTA_BASE = "/inventario-operaciones-ms/v1";
    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    private static final Set<String> TIPOS_MOVIMIENTO = Set.of("ENTRADA", "SALIDA", "AJUSTE", "RESERVA");
    private static final Set<String> TIPOS_ORDEN = Set.of("PRODUCCION", "SERVICIO");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    public HerramientasInventario(RestClient gateway) {
        this(gateway, null);
    }

    public HerramientasInventario(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
    }

    @Tool(name = "consultar_existencias",
          description = "Devuelve las existencias de un artículo por almacén: cantidad, reservado y disponible. Requiere el identificador UUID del artículo.")
    public String consultarExistencias(
            @ToolParam(description = "Identificador UUID del artículo", required = true) String articuloId) {
        RegistroHerramientasUsadas.registrar("consultar_existencias");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/existencias")
                        .queryParam("articulo", UUID.fromString(articuloId.trim()))
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "consultar_trazabilidad_lote",
          description = "Devuelve los movimientos de un lote para trazabilidad: artículo, almacén, tipo y cantidad.")
    public String consultarTrazabilidadLote(
            @ToolParam(description = "Identificador del lote, tal como fue registrado", required = true) String lote) {
        RegistroHerramientasUsadas.registrar("consultar_trazabilidad_lote");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/lotes/{lote}/trazabilidad")
                        .build(lote.trim()))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "listar_articulos_bajo_minimo",
          description = "Lista hasta 15 artículos cuya existencia total está por debajo de su mínimo, del mayor "
                  + "faltante al menor: SKU, nombre, unidad, mínimo, existencia y faltante.")
    public String listarArticulosBajoMinimo() {
        RegistroHerramientasUsadas.registrar("listar_articulos_bajo_minimo");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/articulos/bajo-minimo")
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "listar_movimientos_articulo",
          description = "Lista los últimos 15 movimientos de inventario de un artículo, del más reciente al más "
                  + "antiguo: tipo, cantidad, almacén, lote y referencia.")
    public String listarMovimientosArticulo(
            @ToolParam(description = "Identificador UUID del artículo", required = true) String articuloId) {
        RegistroHerramientasUsadas.registrar("listar_movimientos_articulo");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/movimientos")
                        .queryParam("articulo", UUID.fromString(articuloId.trim()))
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "crear_articulo",
          description = "Prepara el alta de un artículo. NO lo crea: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Solo OPERACIONES puede usarla.")
    public String crearArticulo(
            @ToolParam(description = "SKU del artículo, de hasta 40 caracteres", required = true) String sku,
            @ToolParam(description = "Nombre del artículo, de hasta 150 caracteres", required = true) String nombre,
            @ToolParam(description = "Unidad de medida, de hasta 10 caracteres, por ejemplo PZA o KG", required = true) String unidad,
            @ToolParam(description = "true si el artículo exige lote; false o vacío en otro caso", required = false) String loteObligatorio,
            @ToolParam(description = "Existencia mínima para alertas, cero o positiva; vacío si no aplica", required = false) String minimo) {
        RegistroHerramientasUsadas.registrar("crear_articulo");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        if (vacio(sku) || sku.trim().length() > 40) {
            return "El SKU es obligatorio y de hasta 40 caracteres.";
        }
        if (vacio(nombre) || nombre.trim().length() > 150) {
            return "El nombre es obligatorio y de hasta 150 caracteres.";
        }
        if (vacio(unidad) || unidad.trim().length() > 10) {
            return "La unidad es obligatoria y de hasta 10 caracteres.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sku", sku.trim());
        cuerpo.put("nombre", nombre.trim());
        cuerpo.put("unidad", unidad.trim());
        cuerpo.put("loteObligatorio", "true".equalsIgnoreCase(loteObligatorio == null ? "" : loteObligatorio.trim()));
        if (!vacio(minimo)) {
            BigDecimal valor = decimal(minimo, 3, 9);
            if (valor == null || valor.signum() < 0) {
                return "El mínimo debe ser cero o positivo, con hasta 3 decimales y 9 dígitos enteros.";
            }
            cuerpo.put("minimo", valor);
        }
        String resumen = "Alta de artículo " + sku.trim() + " (" + nombre.trim() + ", " + unidad.trim() + ")";
        return guardar(AccionesPendientes.TIPO_ARTICULO_CREAR, cuerpo, resumen, "Alta preparada, no creada.");
    }

    @Tool(name = "registrar_movimiento_inventario",
          description = "Prepara un movimiento de inventario: entrada, salida, ajuste o reserva. NO lo registra: devuelve "
                  + "un resumen y un accion_id. El usuario debe confirmarlo en la aplicación. Solo ALMACEN puede usarla.")
    public String registrarMovimientoInventario(
            @ToolParam(description = "Identificador UUID del artículo", required = true) String articuloId,
            @ToolParam(description = "Identificador UUID del almacén", required = true) String almacenId,
            @ToolParam(description = "Tipo: ENTRADA, SALIDA, AJUSTE o RESERVA", required = true) String tipo,
            @ToolParam(description = "Cantidad mayor a cero, hasta 3 decimales", required = true) String cantidad,
            @ToolParam(description = "Lote, opcional, de hasta 60 caracteres", required = false) String lote,
            @ToolParam(description = "Referencia, opcional, de hasta 100 caracteres", required = false) String referencia) {
        RegistroHerramientasUsadas.registrar("registrar_movimiento_inventario");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID articulo = identificador(articuloId);
        UUID almacen = identificador(almacenId);
        if (articulo == null || almacen == null) {
            return "El identificador del artículo y del almacén deben ser UUID válidos.";
        }
        String tipoNormalizado = normalizar(tipo);
        if (!TIPOS_MOVIMIENTO.contains(tipoNormalizado)) {
            return "El tipo debe ser ENTRADA, SALIDA, AJUSTE o RESERVA.";
        }
        BigDecimal cant = decimal(cantidad, 3, 9);
        if (cant == null || cant.signum() <= 0) {
            return "La cantidad debe ser mayor a cero, con hasta 3 decimales y 9 dígitos enteros.";
        }
        if (!vacio(lote) && lote.trim().length() > 60) {
            return "El lote no puede tener más de 60 caracteres.";
        }
        if (!vacio(referencia) && referencia.trim().length() > 100) {
            return "La referencia no puede tener más de 100 caracteres.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("articuloId", articulo.toString());
        cuerpo.put("almacenId", almacen.toString());
        cuerpo.put("tipo", tipoNormalizado);
        cuerpo.put("cantidad", cant);
        if (!vacio(lote)) {
            cuerpo.put("lote", lote.trim());
        }
        if (!vacio(referencia)) {
            cuerpo.put("referencia", referencia.trim());
        }
        // Clave única por acción: si el movimiento se reintenta, inventario no lo aplica dos veces.
        cuerpo.put("idempotencyKey", java.util.UUID.randomUUID().toString());
        String resumen = "Movimiento " + tipoNormalizado + " de " + cant.stripTrailingZeros().toPlainString()
                + " del artículo " + articulo + " en el almacén " + almacen;
        return guardar(AccionesPendientes.TIPO_MOVIMIENTO_REGISTRAR, cuerpo, resumen, "Movimiento preparado, no registrado.");
    }

    @Tool(name = "crear_orden_operacion",
          description = "Prepara una orden de operación de producción o de servicio. NO la crea: devuelve un resumen y un "
                  + "accion_id. El usuario debe confirmarlo en la aplicación. Solo OPERACIONES puede usarla.")
    public String crearOrdenOperacion(
            @ToolParam(description = "Tipo de orden: PRODUCCION o SERVICIO", required = true) String tipo,
            @ToolParam(description = "Cantidad objetivo mayor a cero, hasta 3 decimales", required = true) String cantidadObjetivo) {
        RegistroHerramientasUsadas.registrar("crear_orden_operacion");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        String tipoNormalizado = normalizar(tipo);
        if (!TIPOS_ORDEN.contains(tipoNormalizado)) {
            return "El tipo debe ser PRODUCCION o SERVICIO.";
        }
        BigDecimal objetivo = decimal(cantidadObjetivo, 3, 9);
        if (objetivo == null || objetivo.signum() <= 0) {
            return "La cantidad objetivo debe ser mayor a cero, con hasta 3 decimales y 9 dígitos enteros.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipo", tipoNormalizado);
        cuerpo.put("cantidadObjetivo", objetivo);
        String resumen = "Orden de operación " + tipoNormalizado + " por " + objetivo.stripTrailingZeros().toPlainString();
        return guardar(AccionesPendientes.TIPO_ORDEN_OPERACION_CREAR, cuerpo, resumen, "Orden preparada, no creada.");
    }

    private String guardar(String tipo, Map<String, Object> cuerpo, String resumen, String encabezado) {
        AccionPendiente accion = acciones.preparar(usuarioActual(), tipo, aJson(cuerpo), resumen);
        return FiltroDatosSensibles.redactar(encabezado + " " + resumen + ". accion_id: " + accion.id()
                + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.").texto();
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
