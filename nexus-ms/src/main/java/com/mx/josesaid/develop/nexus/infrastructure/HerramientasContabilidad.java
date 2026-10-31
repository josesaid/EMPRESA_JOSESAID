package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre contabilidad-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasContabilidad {

    static final String RUTA_BASE = "/contabilidad-ms/v1";

    private final RestClient gateway;

    public HerramientasContabilidad(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "consultar_poliza",
          description = "Devuelve una póliza contable por su identificador UUID: periodo, tipo, fecha, estatus y movimientos (cargos y abonos por cuenta).")
    public String consultarPoliza(
            @ToolParam(description = "Identificador UUID de la póliza", required = true) String polizaId) {
        RegistroHerramientasUsadas.registrar("consultar_poliza");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/polizas/{id}")
                        .build(UUID.fromString(polizaId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "consultar_estados_financieros",
          description = "Devuelve la balanza de comprobación de un periodo contable: saldos por cuenta. El periodo es AAAA-MM.")
    public String consultarEstadosFinancieros(
            @ToolParam(description = "Periodo contable en formato AAAA-MM, por ejemplo 2026-09", required = true) String periodo) {
        RegistroHerramientasUsadas.registrar("consultar_estados_financieros");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/estados-financieros")
                        .queryParam("periodo", periodo.trim())
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "listar_polizas",
          description = "Lista las pólizas contables de un periodo: identificador, tipo, fecha, estatus y movimientos.")
    public String listarPolizas(
            @ToolParam(description = "Periodo contable en formato AAAA-MM, por ejemplo 2026-09", required = true) String periodo) {
        RegistroHerramientasUsadas.registrar("listar_polizas");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/polizas")
                        .queryParam("periodo", periodo.trim())
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
