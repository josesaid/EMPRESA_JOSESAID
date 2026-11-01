package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

/** Ejecuta acciones confirmadas por gateway-ms con el token del usuario, así cada servicio aplica sus permisos. */
public final class EjecutorAccionesGateway implements EjecutorAcciones {

    static final String RUTA_PEDIDOS = "/pedidos-ventas-ms/v1/pedidos";
    static final String RUTA_PROSPECTOS = "/clientes-ms/v1/prospectos";
    static final String RUTA_COTIZACIONES = "/pedidos-ventas-ms/v1/cotizaciones";
    static final String RUTA_CLIENTES = "/clientes-ms/v1/clientes";
    static final String RUTA_POLIZAS = "/contabilidad-ms/v1/polizas";

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
            case AccionesPendientes.TIPO_COTIZACION_CREAR -> enviar(RUTA_COTIZACIONES, payload);
            case AccionesPendientes.TIPO_PEDIDO_REEMPLAZAR_LINEAS -> {
                String pedidoId = id(payload, "pedidoId");
                yield gateway.put()
                        .uri(RUTA_PEDIDOS + "/" + pedidoId + "/lineas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(sinCampo(payload, "pedidoId"))
                        .retrieve()
                        .body(String.class);
            }
            case AccionesPendientes.TIPO_PEDIDO_DEVOLVER -> {
                String pedidoId = id(payload, "pedidoId");
                yield gateway.post()
                        .uri(RUTA_PEDIDOS + "/" + pedidoId + "/devolucion")
                        .retrieve()
                        .body(String.class);
            }
            case AccionesPendientes.TIPO_PROSPECTO_ESTATUS -> {
                String prospectoId = id(payload, "prospectoId");
                yield gateway.patch()
                        .uri(RUTA_PROSPECTOS + "/" + prospectoId + "/estatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(sinCampo(payload, "prospectoId"))
                        .retrieve()
                        .body(String.class);
            }
            case AccionesPendientes.TIPO_PROSPECTO_CONVERTIR -> {
                String prospectoId = id(payload, "prospectoId");
                yield gateway.post()
                        .uri(RUTA_PROSPECTOS + "/" + prospectoId + "/conversion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(sinCampo(payload, "prospectoId"))
                        .retrieve()
                        .body(String.class);
            }
            case AccionesPendientes.TIPO_INTERACCION_REGISTRAR -> {
                String clienteId = id(payload, "clienteId");
                yield gateway.post()
                        .uri(RUTA_CLIENTES + "/" + clienteId + "/interacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(sinCampo(payload, "clienteId"))
                        .retrieve()
                        .body(String.class);
            }
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
            case AccionesPendientes.TIPO_COTIZACION_ENVIAR -> accionCotizacion(payload, "/envio");
            case AccionesPendientes.TIPO_COTIZACION_ACEPTAR -> accionCotizacion(payload, "/aceptacion");
            case AccionesPendientes.TIPO_COTIZACION_A_PEDIDO -> accionCotizacion(payload, "/pedido");
            case AccionesPendientes.TIPO_POLIZA_REGISTRAR -> enviar(RUTA_POLIZAS, payload);
            default -> throw new IllegalArgumentException("Tipo de acción no soportado: " + tipo);
        };
    }

    private String accionCotizacion(String payload, String accion) {
        String cotizacionId = leer(payload).get("cotizacionId").asText();
        return gateway.post()
                .uri(RUTA_COTIZACIONES + "/" + cotizacionId + accion)
                .retrieve()
                .body(String.class);
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

    private static String id(String payload, String campo) {
        return leer(payload).get(campo).asText();
    }

    /** El cuerpo HTTP es el payload sin el identificador que va en la ruta. */
    private static String sinCampo(String payload, String campo) {
        ObjectNode datos = (ObjectNode) leer(payload);
        datos.remove(campo);
        return datos.toString();
    }
}
