package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre inventario-operaciones-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasInventario {

    static final String RUTA_BASE = "/inventario-operaciones-ms/v1";

    private final RestClient gateway;

    public HerramientasInventario(RestClient gateway) {
        this.gateway = gateway;
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
}
