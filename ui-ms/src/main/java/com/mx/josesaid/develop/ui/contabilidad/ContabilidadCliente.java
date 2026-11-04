package com.mx.josesaid.develop.ui.contabilidad;

import java.util.List;
import java.util.UUID;

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
        List<CuentaDto> cuentas = sesion.ejecutar(token -> gateway.get()
                .uri("/contabilidad-ms/v1/cuentas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<CuentaDto>>() {
                }));
        return cuentas == null ? List.of() : cuentas;
    }

    public List<PolizaDto> listarPolizas(String periodo) {
        List<PolizaDto> polizas = sesion.ejecutar(token -> gateway.get()
                .uri(constructor -> constructor.path("/contabilidad-ms/v1/polizas").queryParam("periodo", periodo).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PolizaDto>>() {
                }));
        return polizas == null ? List.of() : polizas;
    }

    public PolizaDto obtenerPoliza(UUID id) {
        return sesion.ejecutar(token -> gateway.get()
                .uri("/contabilidad-ms/v1/polizas/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(PolizaDto.class));
    }

    public void reversarPoliza(UUID id) {
        sesion.ejecutar(token -> gateway.post()
                .uri("/contabilidad-ms/v1/polizas/{id}/reversa", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }
}
