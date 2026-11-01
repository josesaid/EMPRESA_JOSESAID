package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

/** Ejecuta acciones confirmadas por gateway-ms con el token del usuario, así cada servicio aplica sus permisos. */
public final class EjecutorAccionesGateway implements EjecutorAcciones {

    static final String RUTA_PEDIDOS = "/pedidos-ventas-ms/v1/pedidos";

    private final RestClient gateway;

    public EjecutorAccionesGateway(RestClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public String ejecutar(String tipo, String payload) {
        if (!AccionesPendientes.TIPO_PEDIDO_BORRADOR.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de acción no soportado: " + tipo);
        }
        return gateway.post()
                .uri(RUTA_PEDIDOS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
