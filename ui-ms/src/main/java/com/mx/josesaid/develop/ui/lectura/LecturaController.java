package com.mx.josesaid.develop.ui.lectura;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/** Pantallas de consulta de los módulos. Solo leen; las escrituras viven en Nexus o en sus pantallas propias. */
@Controller
public class LecturaController {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final Pattern LOTE = Pattern.compile("[A-Za-z0-9._-]{1,60}");
    private static final Pattern PERIODO = Pattern.compile("\\d{4}(-(\\d{2}|Q[1-4]))?");
    private static final String SIN_ACCESO = "Tu rol no tiene acceso a esta sección.";
    private static final String ERROR_SERVICIO = "No se pudo consultar el servicio. Intenta más tarde.";
    private static final String ERROR_FILTROS = "La consulta no es válida. Revisa los filtros.";
    private static final String AYUDA = "Captura los filtros para consultar.";

    private final LecturaCliente lectura;

    public LecturaController(LecturaCliente lectura) {
        this.lectura = lectura;
    }

    // ---------- Comercial ----------

    @GetMapping("/clientes")
    public String clientes(@RequestParam(required = false) String rfc,
            @RequestParam(required = false) String nombre, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Clientes", "clientes", "/clientes",
                List.of(campo("rfc", "RFC", rfc, false), campo("nombre", "Nombre", nombre, false)),
                "/clientes-ms/v1/clientes", query("rfc", rfc, "nombre", nombre), "/clientes", null, true);
    }

    @GetMapping("/clientes/{id}")
    public String cliente(@PathVariable UUID id, Authentication usuario, Model modelo) {
        String vista = detalle(modelo, usuario, "Cliente", "clientes", "/clientes", "/clientes",
                "/clientes-ms/v1/clientes/" + id, query(), List.of(), null);
        if (!modelo.containsAttribute("error")) {
            subtabla(modelo, "Interacciones", "/clientes-ms/v1/clientes/" + id + "/interacciones");
        }
        return vista;
    }

    @GetMapping("/ventas/pedidos")
    public String pedidos(@RequestParam(required = false) String clienteId,
            @RequestParam(required = false) String estatus, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Pedidos", "pedidos", "/ventas/pedidos",
                List.of(campo("clienteId", "ID de cliente", clienteId, false), campo("estatus", "Estatus", estatus, false)),
                "/pedidos-ventas-ms/v1/pedidos", query("clienteId", clienteId, "estatus", estatus), "/ventas/pedidos", null, true);
    }

    @GetMapping("/ventas/pedidos/{id}")
    public String pedido(@PathVariable UUID id, Authentication usuario, Model modelo) {
        return detalle(modelo, usuario, "Pedido", "pedidos", "/ventas/pedidos", "/ventas/pedidos",
                "/pedidos-ventas-ms/v1/pedidos/" + id, query(), List.of(), null);
    }

    @GetMapping("/ventas/metricas")
    public String metricasVentas(@RequestParam(required = false) String mes, Authentication usuario, Model modelo) {
        String valor = valorODefault(mes, mesActual());
        return detalle(modelo, usuario, "Métricas de ventas", "metricas", "/ventas/metricas", "/",
                "/pedidos-ventas-ms/v1/metricas/ventas", query("mes", valor),
                List.of(campo("mes", "Mes (AAAA-MM)", valor, true)), null);
    }

    // ---------- Compras ----------

    @GetMapping("/compras/proveedores")
    public String proveedores(@RequestParam(required = false) String razonSocial, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Proveedores", "proveedores", "/compras/proveedores",
                List.of(campo("razonSocial", "Razón social", razonSocial, true)),
                "/compras-proveedores-ms/v1/proveedores", query("razonSocial", razonSocial), "/compras/proveedores",
                null, notBlank(razonSocial));
    }

    @GetMapping("/compras/proveedores/{id}")
    public String proveedor(@PathVariable UUID id, Authentication usuario, Model modelo) {
        return detalle(modelo, usuario, "Proveedor", "proveedores", "/compras/proveedores", "/compras/proveedores",
                "/compras-proveedores-ms/v1/proveedores/" + id, query(), List.of(), null);
    }

    @GetMapping("/compras/ordenes")
    public String ordenes(@RequestParam(required = false) String estatus, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Órdenes de compra", "ordenes", "/compras/ordenes",
                List.of(campo("estatus", "Estatus", estatus, false)),
                "/compras-proveedores-ms/v1/ordenes-compra", query("estatus", estatus), "/compras/ordenes", null, true);
    }

    @GetMapping("/compras/ordenes/{id}")
    public String orden(@PathVariable UUID id, Authentication usuario, Model modelo) {
        return detalle(modelo, usuario, "Orden de compra", "ordenes", "/compras/ordenes", "/compras/ordenes",
                "/compras-proveedores-ms/v1/ordenes-compra/" + id, query(), List.of(), null);
    }

    // ---------- Inventario ----------

    @GetMapping("/inventario/existencias")
    public String existencias(@RequestParam(required = false) String articulo, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Existencias", "existencias", "/inventario/existencias",
                List.of(campo("articulo", "ID de artículo", articulo, true)),
                "/inventario-operaciones-ms/v1/existencias", query("articulo", articulo), null, null, notBlank(articulo));
    }

    @GetMapping("/inventario/bajo-minimo")
    public String bajoMinimo(Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Artículos bajo mínimo", "bajo-minimo", "/inventario/bajo-minimo", List.of(),
                "/inventario-operaciones-ms/v1/articulos/bajo-minimo", query(), null, null, true);
    }

    @GetMapping("/inventario/movimientos")
    public String movimientos(@RequestParam(required = false) String articulo, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Movimientos de inventario", "movimientos", "/inventario/movimientos",
                List.of(campo("articulo", "ID de artículo", articulo, true)),
                "/inventario-operaciones-ms/v1/movimientos", query("articulo", articulo), null, null, notBlank(articulo));
    }

    @GetMapping("/inventario/trazabilidad")
    public String trazabilidad(@RequestParam(required = false) String lote, Authentication usuario, Model modelo) {
        boolean valido = lote == null || lote.isBlank() || LOTE.matcher(lote).matches();
        boolean ejecutar = notBlank(lote) && valido;
        String ruta = ejecutar ? "/inventario-operaciones-ms/v1/lotes/" + lote + "/trazabilidad" : "";
        return listado(modelo, usuario, "Trazabilidad de lote", "trazabilidad", "/inventario/trazabilidad",
                List.of(campo("lote", "Lote", lote, true)), ruta, query(), null,
                valido ? null : "El lote tiene caracteres no válidos.", ejecutar);
    }

    // ---------- Finanzas ----------

    @GetMapping("/contabilidad/estados-financieros")
    public String estadosFinancieros(@RequestParam(required = false) String periodo, Authentication usuario, Model modelo) {
        String valor = valorODefault(periodo, mesActual());
        return detalle(modelo, usuario, "Estados financieros", "estados", "/contabilidad/estados-financieros", "/",
                "/contabilidad-ms/v1/estados-financieros", query("periodo", valor),
                List.of(campo("periodo", "Periodo (AAAA-MM)", valor, true)), null);
    }

    @GetMapping("/facturacion/comprobantes")
    public String comprobantes(@RequestParam(required = false) String folio, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Comprobantes", "comprobantes", "/facturacion/comprobantes",
                List.of(campo("folio", "Folio", folio, true)), "/facturacion-pagos-ms/v1/comprobantes",
                query("folio", folio), "/facturacion/comprobantes", null, notBlank(folio));
    }

    @GetMapping("/facturacion/comprobantes/{id}")
    public String comprobante(@PathVariable UUID id, Authentication usuario, Model modelo) {
        return detalle(modelo, usuario, "Comprobante", "comprobantes", "/facturacion/comprobantes",
                "/facturacion/comprobantes", "/facturacion-pagos-ms/v1/comprobantes/" + id, query(), List.of(), null);
    }

    @GetMapping("/facturacion/pendientes-cobro")
    public String pendientesCobro(Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Pendientes de cobro", "pendientes-cobro", "/facturacion/pendientes-cobro", List.of(),
                "/facturacion-pagos-ms/v1/comprobantes/pendientes-cobro", query(), "/facturacion/comprobantes", null, true);
    }

    @GetMapping("/facturacion/pagos-proveedor")
    public String pagosProveedor(Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Pagos a proveedores", "pagos-proveedor", "/facturacion/pagos-proveedor", List.of(),
                "/facturacion-pagos-ms/v1/cuentas-por-pagar/pagos", query(), null, null, true);
    }

    // ---------- Cumplimiento ----------

    @GetMapping("/documentos")
    public String documentos(@RequestParam(required = false) String titulo, Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Documentos", "documentos", "/documentos",
                List.of(campo("titulo", "Título", titulo, true)), "/documentos-cumplimiento-ms/v1/documentos",
                query("titulo", titulo), "/documentos", null, notBlank(titulo));
    }

    @GetMapping("/documentos/{id}")
    public String documento(@PathVariable UUID id, Authentication usuario, Model modelo) {
        return detalle(modelo, usuario, "Documento", "documentos", "/documentos", "/documentos",
                "/documentos-cumplimiento-ms/v1/documentos/" + id, query(), List.of(), null);
    }

    @GetMapping("/documentos/por-vencer")
    public String porVencer(@RequestParam(required = false) String dias, Authentication usuario, Model modelo) {
        String valor = valorODefault(dias, "30");
        return listado(modelo, usuario, "Documentos por vencer", "por-vencer", "/documentos/por-vencer",
                List.of(campo("dias", "Días", valor, true)), "/documentos-cumplimiento-ms/v1/documentos/por-vencer",
                query("dias", valor), "/documentos", null, true);
    }

    // ---------- Dirección ----------

    @GetMapping("/reportes/indicadores")
    public String indicadores(@RequestParam(required = false) String periodo, @RequestParam(required = false) String area,
            Authentication usuario, Model modelo) {
        String valor = valorODefault(periodo, mesActual());
        String validacion = PERIODO.matcher(valor).matches() ? null : "El periodo debe tener formato AAAA, AAAA-Qn o AAAA-MM.";
        return listado(modelo, usuario, "Indicadores", "indicadores", "/reportes/indicadores",
                List.of(campo("periodo", "Periodo", valor, true), campo("area", "Área", area, false)),
                "/reportes-ejecutivos-ms/v1/indicadores", query("periodo", valor, "area", area), null, validacion, true);
    }

    @GetMapping("/reportes/reporte")
    public String reporte(@RequestParam(required = false) String periodo, Authentication usuario, Model modelo) {
        String valor = valorODefault(periodo, mesActual());
        boolean valido = PERIODO.matcher(valor).matches();
        return detalle(modelo, usuario, "Reporte por periodo", "reporte", "/reportes/reporte", "/",
                "/reportes-ejecutivos-ms/v1/reportes/" + (valido ? valor : "0000"), query(),
                List.of(campo("periodo", "Periodo", valor, true)),
                valido ? null : "El periodo debe tener formato AAAA, AAAA-Qn o AAAA-MM.");
    }

    // ---------- Accesos ----------

    @GetMapping("/usuarios")
    public String usuarios(Authentication usuario, Model modelo) {
        return listado(modelo, usuario, "Usuarios", "usuarios", "/usuarios", List.of(),
                "/usuarios-accesos-ms/v1/usuarios", query(), "/usuarios", null, true);
    }

    @GetMapping("/usuarios/{id}")
    public String usuario(@PathVariable UUID id, Authentication usuario, Model modelo) {
        return detalle(modelo, usuario, "Usuario", "usuarios", "/usuarios", "/usuarios",
                "/usuarios-accesos-ms/v1/usuarios/" + id, query(), List.of(), null);
    }

    // ---------- Vistas comunes ----------

    private String listado(Model modelo, Authentication usuario, String titulo, String seccion, String rutaVista,
            List<Map<String, String>> campos, String rutaApi, MultiValueMap<String, String> q, String enlace,
            String validacion, boolean ejecutar) {
        base(modelo, usuario, titulo, seccion);
        modelo.addAttribute("rutaVista", rutaVista);
        modelo.addAttribute("campos", campos);
        modelo.addAttribute("ayuda", AYUDA);
        modelo.addAttribute("ejecutado", ejecutar && validacion == null);
        if (validacion != null) {
            modelo.addAttribute("error", validacion);
        } else if (ejecutar) {
            Object cuerpo = consultar(modelo, "error", rutaApi, q);
            if (!modelo.containsAttribute("error")) {
                modelo.addAttribute("columnas", columnas(cuerpo));
                modelo.addAttribute("filas", filas(cuerpo, enlace));
            }
        }
        return "lectura/tabla";
    }

    private String detalle(Model modelo, Authentication usuario, String titulo, String seccion, String rutaVista,
            String volver, String rutaApi, MultiValueMap<String, String> q, List<Map<String, String>> campos,
            String validacion) {
        base(modelo, usuario, titulo, seccion);
        modelo.addAttribute("rutaVista", rutaVista);
        modelo.addAttribute("volver", volver);
        modelo.addAttribute("campos", campos);
        if (validacion != null) {
            modelo.addAttribute("error", validacion);
        } else {
            Object cuerpo = consultar(modelo, "error", rutaApi, q);
            if (!modelo.containsAttribute("error")) {
                modelo.addAttribute("detalle", entradas(cuerpo));
            }
        }
        return "lectura/detalle";
    }

    private void subtabla(Model modelo, String titulo, String ruta) {
        Object cuerpo = consultar(modelo, "errorSub", ruta, query());
        if (!modelo.containsAttribute("errorSub")) {
            modelo.addAttribute("subtitulo", titulo);
            modelo.addAttribute("subcolumnas", columnas(cuerpo));
            modelo.addAttribute("subfilas", filas(cuerpo, null));
        }
    }

    private void base(Model modelo, Authentication usuario, String titulo, String seccion) {
        modelo.addAttribute("titulo", titulo);
        modelo.addAttribute("seccion", seccion);
        modelo.addAttribute("correo", usuario.getName());
    }

    private Object consultar(Model modelo, String clave, String ruta, MultiValueMap<String, String> q) {
        try {
            return lectura.leer(ruta, q);
        } catch (RestClientException e) {
            modelo.addAttribute(clave, mensaje(e));
            return null;
        }
    }

    private static String mensaje(RestClientException e) {
        if (e instanceof HttpClientErrorException.Forbidden) {
            return SIN_ACCESO;
        }
        if (e instanceof HttpClientErrorException.NotFound) {
            return "No se encontró el registro.";
        }
        if (e instanceof HttpClientErrorException) {
            return ERROR_FILTROS;
        }
        return ERROR_SERVICIO;
    }

    // ---------- Utilidades ----------

    private static Map<String, String> campo(String nombre, String etiqueta, String valor, boolean requerido) {
        return Map.of("nombre", nombre, "etiqueta", etiqueta, "valor", valor == null ? "" : valor,
                "requerido", String.valueOf(requerido));
    }

    private static MultiValueMap<String, String> query(String... pares) {
        MultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            if (pares[i + 1] != null && !pares[i + 1].isBlank()) {
                q.add(pares[i], pares[i + 1].trim());
            }
        }
        return q;
    }

    private static boolean notBlank(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String valorODefault(String valor, String predeterminado) {
        return valor == null || valor.isBlank() ? predeterminado : valor.trim();
    }

    private static String mesActual() {
        return YearMonth.now(ZONA).toString();
    }

    private static List<?> elementos(Object cuerpo) {
        List<?> brutos;
        if (cuerpo instanceof List<?> lista) {
            brutos = lista;
        } else if (cuerpo instanceof Map<?, ?> mapa && mapa.get("content") instanceof List<?> contenido) {
            brutos = contenido;
        } else if (cuerpo instanceof Map<?, ?>) {
            brutos = List.of(cuerpo);
        } else {
            return List.of();
        }
        return brutos.stream().map(LecturaController::aplanarFila).toList();
    }

    /**
     * Sube los campos escalares de un objeto anidado (por ejemplo {@code cliente}) al nivel de la fila,
     * para que la tabla muestre sus columnas y el {@code id} genere el enlace al detalle.
     */
    private static Object aplanarFila(Object elemento) {
        if (!(elemento instanceof Map<?, ?> mapa)) {
            return elemento;
        }
        Map<Object, Object> fila = new LinkedHashMap<>();
        mapa.forEach((clave, valor) -> {
            if (valor instanceof Map<?, ?> anidado) {
                anidado.forEach((claveAnidada, valorAnidado) -> {
                    if (!(valorAnidado instanceof Map<?, ?>) && !(valorAnidado instanceof List<?>)) {
                        fila.putIfAbsent(claveAnidada, valorAnidado);
                    }
                });
            } else {
                fila.put(clave, valor);
            }
        });
        return fila;
    }

    private static List<Map<String, String>> columnas(Object cuerpo) {
        List<Map<String, String>> columnas = new ArrayList<>();
        List<?> elementos = elementos(cuerpo);
        if (!elementos.isEmpty() && elementos.get(0) instanceof Map<?, ?> primera) {
            primera.forEach((clave, valor) -> {
                if (!"id".equals(clave) && !(valor instanceof Map<?, ?>) && !(valor instanceof List<?>)
                        && tieneValor(elementos, clave)) {
                    columnas.add(Map.of("nombre", String.valueOf(clave), "etiqueta", etiqueta(String.valueOf(clave))));
                }
            });
        }
        return columnas;
    }

    /** Una columna solo se muestra si al menos una fila tiene valor; así no aparecen columnas vacías. */
    private static boolean tieneValor(List<?> elementos, Object clave) {
        return elementos.stream().anyMatch(e -> e instanceof Map<?, ?> mapa && mapa.get(clave) != null);
    }

    private static List<Map<String, String>> filas(Object cuerpo, String enlace) {
        List<Map<String, String>> filas = new ArrayList<>();
        for (Object elemento : elementos(cuerpo)) {
            if (elemento instanceof Map<?, ?> mapa) {
                Map<String, String> fila = new LinkedHashMap<>();
                mapa.forEach((clave, valor) -> fila.put(String.valueOf(clave), texto(valor)));
                if (enlace != null && mapa.get("id") != null) {
                    fila.put("_enlace", enlace + "/" + mapa.get("id"));
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    private static List<Map<String, String>> entradas(Object cuerpo) {
        List<Map<String, String>> entradas = new ArrayList<>();
        if (cuerpo instanceof Map<?, ?> mapa) {
            mapa.forEach((clave, valor) -> entradas.add(
                    Map.of("etiqueta", etiqueta(String.valueOf(clave)), "valor", texto(valor))));
        }
        return entradas;
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return "—";
        }
        if (valor instanceof Boolean booleano) {
            return booleano ? "Sí" : "No";
        }
        if (valor instanceof Map<?, ?> mapa) {
            return mapa.entrySet().stream()
                    .map(e -> etiqueta(String.valueOf(e.getKey())) + ": " + texto(e.getValue()))
                    .collect(Collectors.joining(", "));
        }
        if (valor instanceof List<?> lista) {
            return lista.size() + (lista.size() == 1 ? " elemento" : " elementos");
        }
        return String.valueOf(valor);
    }

    /** Convierte una clave tipo "fechaCreacion" en "Fecha creación". */
    private static String etiqueta(String clave) {
        String texto = clave.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.ROOT);
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
