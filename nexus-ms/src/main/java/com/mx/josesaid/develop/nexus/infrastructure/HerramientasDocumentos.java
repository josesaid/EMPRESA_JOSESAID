package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre documentos-cumplimiento-ms, por gateway-ms con el token del usuario.
 * Solo se consulta metadato: la descarga de archivos no se ofrece al modelo.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasDocumentos {

    static final String RUTA_BASE = "/documentos-cumplimiento-ms/v1";

    private final RestClient gateway;

    public HerramientasDocumentos(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "consultar_documento",
          description = "Devuelve los datos de un documento legal por su identificador UUID: título, tipo, área, fecha de vencimiento y versiones (número, hash y fecha). No devuelve el contenido del archivo.")
    public String consultarDocumento(
            @ToolParam(description = "Identificador UUID del documento", required = true) String documentoId) {
        RegistroHerramientasUsadas.registrar("consultar_documento");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/documentos/{id}")
                        .build(UUID.fromString(documentoId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "consultar_documentos_por_vencer",
          description = "Lista los documentos que vencen en los próximos N días.")
    public String consultarDocumentosPorVencer(
            @ToolParam(description = "Número de días hacia adelante, por ejemplo 30", required = true) int dias) {
        RegistroHerramientasUsadas.registrar("consultar_documentos_por_vencer");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/documentos/por-vencer")
                        .queryParam("dias", dias)
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "buscar_documentos",
          description = "Busca documentos legales no eliminados cuyo título contiene el texto (sin distinguir "
                  + "mayúsculas). Devuelve hasta 15 documentos por título, con tipo, área, vencimiento y versiones. "
                  + "Pide al menos 3 caracteres.")
    public String buscarDocumentos(
            @ToolParam(description = "Texto que debe contener el título del documento (mínimo 3 caracteres)",
                       required = true) String texto) {
        RegistroHerramientasUsadas.registrar("buscar_documentos");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/documentos")
                        .queryParam("titulo", texto.trim())
                        .build())
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
