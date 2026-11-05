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

    public NexusCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    public SugerenciaDto sugerirPoliza(String descripcion) {
        return sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/polizas/sugerencias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("descripcion", descripcion))
                .retrieve()
                .body(SugerenciaDto.class));
    }

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

    public void verificarAlta(UUID id, String codigo) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/alta/verificacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("codigo", codigo))
                .retrieve()
                .toBodilessEntity());
    }

    public void completarAlta(UUID id, String codigo) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/alta/completacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("codigo", codigo))
                .retrieve()
                .toBodilessEntity());
    }

    public void cancelar(UUID id) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/nexus/acciones/{id}/cancelacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }
}
