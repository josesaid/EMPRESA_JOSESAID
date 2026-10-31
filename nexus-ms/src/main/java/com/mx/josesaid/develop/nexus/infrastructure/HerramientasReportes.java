package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre reportes-ejecutivos-ms.
 * Llaman por gateway-ms con el token del usuario; el servicio aplica sus propios permisos.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasReportes {

    static final String RUTA_BASE = "/reportes-ejecutivos-ms/v1";

    private final RestClient gateway;

    public HerramientasReportes(RestClient gateway) {
        this.gateway = gateway;
    }

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
}
