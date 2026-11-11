package com.mx.josesaid.develop.nexus.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
 * Herramientas de solo lectura sobre reportes-ejecutivos-ms.
 * Llaman por gateway-ms con el token del usuario; el servicio aplica sus propios permisos.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasReportes {

    static final String RUTA_BASE = "/reportes-ejecutivos-ms/v1";

    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    /** Áreas válidas para consultar reportes ejecutivos. */
    private static final List<String> AREAS = List.of("VENTAS_ATENCION_CLIENTES", "FINANZAS_CONTABILIDAD",
            "OPERACIONES", "COMPRAS_PROVEEDORES", "ADMINISTRACION_LEGAL", "DIRECCION_GENERAL");
    private static final Pattern PERIODO = Pattern.compile("^(\\d{4}-(0[1-9]|1[0-2])|\\d{4}-Q[1-4]|\\d{4})$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    /** Versión sin registro de acciones: solo permite consultar. */
    public HerramientasReportes(RestClient gateway) {
        this(gateway, null);
    }

    /** Recibe el cliente del gateway y el registro de acciones. */
    public HerramientasReportes(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
    }

    /** Herramienta del asistente: devuelve los indicadores de un área y periodo. */
    @Tool(name = "consultar_indicadores",
          description = "Devuelve los indicadores consolidados de un periodo. El periodo es AAAA-MM, AAAA-Qn o AAAA.")
    public String consultarIndicadores(
            @ToolParam(description = "Periodo: AAAA-MM, AAAA-Q1..Q4 o AAAA", required = true) String periodo,
            @ToolParam(description = "Área opcional. Valores válidos: VENTAS_ATENCION_CLIENTES, FINANZAS_CONTABILIDAD, OPERACIONES, COMPRAS_PROVEEDORES, ADMINISTRACION_LEGAL, DIRECCION_GENERAL. Vacío para todas.",
                       required = false) String area) {
        RegistroHerramientasUsadas.registrar("consultar_indicadores");
        String cuerpo = gateway.get()
                .uri(constructor -> {
                    var uri = constructor.path(RUTA_BASE + "/indicadores").queryParam("periodo", periodo);
                    if (area != null && !area.isBlank()) {
                        uri.queryParam("area", area.trim().toUpperCase());
                    }
                    return uri.build();
                })
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    /** Herramienta del asistente: devuelve un reporte ejecutivo del área y periodo indicados. */
    @Tool(name = "consultar_reporte",
          description = "Devuelve el reporte ejecutivo de un periodo (AAAA-MM, AAAA-Qn o AAAA) con su estatus.")
    public String consultarReporte(
            @ToolParam(description = "Periodo: AAAA-MM, AAAA-Q1..Q4 o AAAA", required = true) String periodo) {
        RegistroHerramientasUsadas.registrar("consultar_reporte");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/reportes/{periodo}", periodo)
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    /** Herramienta del asistente: prepara el registro de un indicador. Requiere confirmación del usuario. */
    @Tool(name = "registrar_indicador",
          description = "Prepara el registro de un indicador de un área para un periodo. NO lo registra: devuelve "
                  + "un resumen y un accion_id. El usuario debe confirmarlo en la aplicación. Solo DIRECCION puede usarla.")
    public String registrarIndicador(
            @ToolParam(description = "Área. Valores válidos: VENTAS_ATENCION_CLIENTES, FINANZAS_CONTABILIDAD, OPERACIONES, COMPRAS_PROVEEDORES, ADMINISTRACION_LEGAL, DIRECCION_GENERAL",
                       required = true) String area,
            @ToolParam(description = "Nombre del indicador", required = true) String nombre,
            @ToolParam(description = "Periodo: AAAA-MM, AAAA-Q1..Q4 o AAAA", required = true) String periodo,
            @ToolParam(description = "Valor numérico del indicador", required = true) String valor,
            @ToolParam(description = "Momento en que se actualizó la fuente, en ISO-8601 UTC, por ejemplo 2026-11-08T00:00:00Z",
                       required = true) String fuenteActualizadaUtc) {
        RegistroHerramientasUsadas.registrar("registrar_indicador");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        String areaLimpia = area == null ? "" : area.trim().toUpperCase();
        if (!AREAS.contains(areaLimpia)) {
            return "El área no es válida. Valores: " + String.join(", ", AREAS) + ".";
        }
        if (nombre == null || nombre.isBlank()) {
            return "El nombre del indicador es obligatorio.";
        }
        String periodoLimpio = periodo == null ? "" : periodo.trim();
        if (!PERIODO.matcher(periodoLimpio).matches()) {
            return "El periodo debe ser AAAA-MM, AAAA-Qn o AAAA.";
        }
        BigDecimal numero;
        try {
            numero = new BigDecimal(valor.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return "El valor debe ser numérico.";
        }
        Instant fuente;
        try {
            fuente = Instant.parse(fuenteActualizadaUtc.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            return "La fecha de actualización debe estar en ISO-8601 UTC, por ejemplo 2026-11-08T00:00:00Z.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("area", areaLimpia);
        cuerpo.put("nombre", nombre.trim());
        cuerpo.put("periodo", periodoLimpio);
        cuerpo.put("valor", numero);
        cuerpo.put("fuenteActualizadaUtc", fuente.toString());
        String resumen = "Registrar indicador " + nombre.trim() + " de " + areaLimpia + " (" + periodoLimpio
                + ") = " + numero.toPlainString();
        AccionPendiente accion = acciones.preparar(usuarioActual(), AccionesPendientes.TIPO_INDICADOR_REGISTRAR,
                aJson(cuerpo), resumen);
        return FiltroDatosSensibles.redactar("Registro preparado, no ejecutado. " + resumen
                + ". accion_id: " + accion.id() + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.")
                .texto();
    }

    /** Devuelve el nombre del usuario autenticado. */
    private static String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Convierte un mapa en JSON para guardarlo en la acción pendiente. */
    private static String aJson(Map<String, Object> datos) {
        try {
            return JSON.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la acción", e);
        }
    }
}
