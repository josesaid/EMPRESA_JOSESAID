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

    /** Recibe el cliente del gateway y el servicio de sesión para llamar a las APIs con el token del usuario. */
    public AltasCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    /** Da de alta un cliente en comercial a través del gateway. */
    public Object crearCliente(Map<String, Object> cuerpo) {
        return alta("/clientes-ms/v1/clientes", cuerpo);
    }

    /** Da de alta un proveedor en compras a través del gateway. */
    public Object crearProveedor(Map<String, Object> cuerpo) {
        return alta("/compras-proveedores-ms/v1/proveedores", cuerpo);
    }

    /** Envía el alta a la ruta indicada. Si el servicio responde con error, la excepción sube a la capa web. */
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
