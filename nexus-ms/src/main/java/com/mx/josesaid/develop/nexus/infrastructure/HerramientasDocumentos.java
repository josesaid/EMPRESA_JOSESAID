package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas sobre documentos-cumplimiento-ms, por gateway-ms con el token del usuario.
 * Las de lectura solo consultan metadato: la descarga de archivos no se ofrece al modelo.
 * La eliminación no se ejecuta aquí: prepara una acción que el usuario confirma en la aplicación.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasDocumentos {

    static final String RUTA_BASE = "/documentos-cumplimiento-ms/v1";
    static final int LIMITE_MOTIVO = 300;
    private static final String NO_DISPONIBLE = "La eliminación de documentos no está disponible en este momento.";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    public HerramientasDocumentos(RestClient gateway) {
        this(gateway, null);
    }

    public HerramientasDocumentos(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
    }

    @Tool(name = "consultar_tipos_documento",
          description = "Devuelve el catálogo de tipos de documento legal que acepta el sistema.")
    public String consultarTiposDocumento() {
        RegistroHerramientasUsadas.registrar("consultar_tipos_documento");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/catalogos/tipos-documento")
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "solicitar_eliminacion_documento",
          description = "Prepara la eliminación de un documento legal. NO lo elimina: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. No se puede eliminar un documento dentro de su "
                  + "periodo de retención ni uno ya eliminado; en esos casos la aplicación lo rechaza al confirmar.")
    public String solicitarEliminacionDocumento(
            @ToolParam(description = "Identificador UUID del documento", required = true) String documentoId,
            @ToolParam(description = "Motivo de la eliminación, obligatorio, de hasta 300 caracteres",
                       required = true) String motivo) {
        RegistroHerramientasUsadas.registrar("solicitar_eliminacion_documento");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id;
        try {
            id = UUID.fromString(documentoId.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            return "El identificador del documento no es válido.";
        }
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) {
            return "El motivo de la eliminación es obligatorio.";
        }
        if (motivoLimpio.length() > LIMITE_MOTIVO) {
            return "El motivo no puede tener más de " + LIMITE_MOTIVO + " caracteres.";
        }
        if (!existe(id)) {
            return "No encontré ese documento.";
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("documentoId", id.toString());
        cuerpo.put("motivo", motivoLimpio);
        String resumen = "Eliminar documento " + id + " (motivo: " + motivoLimpio + ")";
        AccionPendiente accion = acciones.preparar(usuarioActual(), AccionesPendientes.TIPO_DOCUMENTO_ELIMINAR,
                aJson(cuerpo), resumen);
        return FiltroDatosSensibles.redactar("Eliminación preparada, no ejecutada. " + resumen
                + ". accion_id: " + accion.id() + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.")
                .texto();
    }

    private boolean existe(UUID id) {
        try {
            gateway.get().uri(RUTA_BASE + "/documentos/" + id).retrieve().body(String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
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
