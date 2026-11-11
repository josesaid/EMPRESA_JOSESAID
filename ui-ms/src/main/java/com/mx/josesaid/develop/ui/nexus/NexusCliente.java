package com.mx.josesaid.develop.ui.nexus;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.ui.seguridad.ServicioSesion;

/** Cliente de nexus-ms para las acciones de pólizas. Siempre envía el token vigente del usuario. */
@Component
public class NexusCliente {

    private final RestClient gateway;
    private final ServicioSesion sesion;

    /** Recibe el cliente del gateway y el servicio de sesión. */
    public NexusCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    /** Pide a Nexus una sugerencia de póliza a partir de una descripción en lenguaje natural. Solo sugiere; no registra nada. */
    public SugerenciaDto sugerirPoliza(String descripcion) {
        return sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/polizas/sugerencias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("descripcion", descripcion))
                .retrieve()
                .body(SugerenciaDto.class));
    }

    /** Pregunta libre al asistente Nexus. El asistente decide qué herramientas usar. */
    public String consultar(String pregunta) {
        RespuestaConsulta r = sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/consultas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("pregunta", pregunta))
                .retrieve()
                .body(RespuestaConsulta.class));
        return r == null || r.respuesta() == null ? "" : r.respuesta();
    }

    /**
     * Respuesta de nexus-ms a una consulta.
     *
     * @param respuesta respuesta, de tipo {@code String}.
     */
    public record RespuestaConsulta(String respuesta) {
    }

    /** Confirma una acción pendiente con su código de verificación. */
    public void confirmar(UUID id, String codigo) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/confirmacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("codigo", codigo))
                .retrieve()
                .toBodilessEntity());
    }

    /** Datos del alta pendiente. La contraseña nunca pasa por Nexus. */
    public DatosAltaDto datosAlta(UUID id) {
        return sesion.ejecutar(token -> gateway.get()
                .uri("/nexus/acciones/{id}/alta", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(DatosAltaDto.class));
    }

    /** Verifica el código de un alta pendiente, sin completarla. */
    public void verificarAlta(UUID id, String codigo) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/alta/verificacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("codigo", codigo))
                .retrieve()
                .toBodilessEntity());
    }

    /** Completa un alta pendiente con su código de verificación. */
    public void completarAlta(UUID id, String codigo) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/alta/completacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("codigo", codigo))
                .retrieve()
                .toBodilessEntity());
    }

    /** Cancela una acción pendiente. */
    public void cancelar(UUID id) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/cancelacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }
}
