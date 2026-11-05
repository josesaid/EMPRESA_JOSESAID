package com.mx.josesaid.develop.ui.usuarios;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.ui.nexus.DatosAltaDto;
import com.mx.josesaid.develop.ui.seguridad.ServicioSesion;

/**
 * Crea el usuario en usuarios-accesos-ms con el token del administrador. La contraseña viaja solo en esta
 * llamada: no se guarda, no se registra y no pasa por Nexus.
 */
@Component
public class UsuariosCliente {

    private final RestClient gateway;
    private final ServicioSesion sesion;

    public UsuariosCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    public void crear(DatosAltaDto datos, String password) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("correo", datos.correo());
        cuerpo.put("nombreCompleto", datos.nombreCompleto());
        cuerpo.put("password", password);
        cuerpo.put("roles", datos.roles());
        sesion.ejecutar(token -> gateway.post()
                .uri("/usuarios-accesos-ms/v1/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity());
    }
}
