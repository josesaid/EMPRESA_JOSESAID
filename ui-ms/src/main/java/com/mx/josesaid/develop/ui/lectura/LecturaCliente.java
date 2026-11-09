package com.mx.josesaid.develop.ui.lectura;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.ui.seguridad.ServicioSesion;

/** Lee cualquier ruta del gateway con el token vigente del usuario. Devuelve un Map, una List o null. */
@Component
public class LecturaCliente {

    private final RestClient gateway;
    private final ServicioSesion sesion;

    public LecturaCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    public Object leer(String ruta, MultiValueMap<String, String> query) {
        return sesion.ejecutar(token -> gateway.get()
                .uri(constructor -> constructor.path(ruta).queryParams(query).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(Object.class));
    }
}
