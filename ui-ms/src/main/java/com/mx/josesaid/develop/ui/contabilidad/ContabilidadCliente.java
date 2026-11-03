package com.mx.josesaid.develop.ui.contabilidad;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.ui.seguridad.ServicioSesion;

/** Cliente de contabilidad-ms. Siempre envía el token vigente del usuario. */
@Component
public class ContabilidadCliente {

    private final RestClient gateway;
    private final ServicioSesion sesion;

    public ContabilidadCliente(RestClient gatewayClient, ServicioSesion sesion) {
        this.gateway = gatewayClient;
        this.sesion = sesion;
    }

    public List<CuentaDto> listarCuentas() {
        List<CuentaDto> cuentas = gateway.get()
                .uri("/contabilidad-ms/v1/cuentas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.accessTokenVigente())
                .retrieve()
                .body(new ParameterizedTypeReference<List<CuentaDto>>() {
                });
        return cuentas == null ? List.of() : cuentas;
    }
}
