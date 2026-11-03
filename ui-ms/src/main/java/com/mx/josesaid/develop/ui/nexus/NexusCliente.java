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
        return gateway.post()
                .uri("/nexus/polizas/sugerencias")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.accessTokenVigente())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("descripcion", descripcion))
                .retrieve()
                .body(SugerenciaDto.class);
    }

    public void confirmar(UUID id, String codigo) {
        gateway.post()
                .uri("/nexus/acciones/{id}/confirmacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.accessTokenVigente())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("codigo", codigo))
                .retrieve()
                .toBodilessEntity();
    }

    public void cancelar(UUID id) {
        gateway.post()
                .uri("/nexus/acciones/{id}/cancelacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.accessTokenVigente())
                .retrieve()
                .toBodilessEntity();
    }
}
