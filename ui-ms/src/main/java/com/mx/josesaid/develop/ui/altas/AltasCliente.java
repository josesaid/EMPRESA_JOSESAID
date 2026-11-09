package com.mx.josesaid.develop.ui.altas;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.ui.seguridad.ServicioSesion;

/** Da de alta clientes y proveedores en su microservicio, con el token del usuario que hace el alta. */
@Component
public class AltasCliente {

    private final RestClient gateway;
    private final ServicioSesion sesion;

    public AltasCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    public Object crearCliente(Map<String, Object> cuerpo) {
        return alta("/clientes-ms/v1/clientes", cuerpo);
    }

    public Object crearProveedor(Map<String, Object> cuerpo) {
        return alta("/compras-proveedores-ms/v1/proveedores", cuerpo);
    }

    private Object alta(String ruta, Map<String, Object> cuerpo) {
        return sesion.ejecutar(token -> gateway.post()
                .uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(Object.class));
    }
}
