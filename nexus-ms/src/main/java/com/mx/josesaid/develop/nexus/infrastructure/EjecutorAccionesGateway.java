package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

/** Ejecuta acciones confirmadas por gateway-ms con el token del usuario, así cada servicio aplica sus permisos. */
public final class EjecutorAccionesGateway implements EjecutorAcciones {

    static final String RUTA_PEDIDOS = "/pedidos-ventas-ms/v1/pedidos";
    static final String RUTA_PROSPECTOS = "/clientes-ms/v1/prospectos";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;

    public EjecutorAccionesGateway(RestClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public String ejecutar(String tipo, String payload) {
        return switch (tipo) {
            case AccionesPendientes.TIPO_PEDIDO_BORRADOR -> enviar(RUTA_PEDIDOS, payload);
            case AccionesPendientes.TIPO_PROSPECTO_BORRADOR -> enviar(RUTA_PROSPECTOS, payload);
            case AccionesPendientes.TIPO_PEDIDO_CONFIRMAR -> {
                String pedidoId = leer(payload).get("pedidoId").asText();
                yield gateway.post()
                        .uri(RUTA_PEDIDOS + "/" + pedidoId + "/confirmacion")
                        .retrieve()
                        .body(String.class);
            }
            case AccionesPendientes.TIPO_PEDIDO_CANCELAR -> {
                JsonNode datos = leer(payload);
                String cuerpo = "{\"motivo\":\"" + datos.get("motivo").asText() + "\"}";
                yield gateway.post()
                        .uri(RUTA_PEDIDOS + "/" + datos.get("pedidoId").asText() + "/cancelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)
                        .retrieve()
                        .body(String.class);
            }
            default -> throw new IllegalArgumentException("Tipo de acción no soportado: " + tipo);
        };
    }

    private String enviar(String ruta, String payload) {
        return gateway.post()
                .uri(ruta)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    private static JsonNode leer(String payload) {
        try {
            return JSON.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload de acción inválido", e);
        }
    }
}
