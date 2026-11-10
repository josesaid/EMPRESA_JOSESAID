package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.Map;

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
    static final String RUTA_DOCUMENTOS = "/documentos-cumplimiento-ms/v1/documentos";
    static final String RUTA_PROVEEDORES = "/compras-proveedores-ms/v1/proveedores";
    static final String RUTA_ORDENES_COMPRA = "/compras-proveedores-ms/v1/ordenes-compra";
    static final String RUTA_INVENTARIO = "/inventario-operaciones-ms/v1";
    static final String RUTA_FACTURACION = "/facturacion-pagos-ms/v1";
    static final String RUTA_REPORTES = "/reportes-ejecutivos-ms/v1";

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
            case AccionesPendientes.TIPO_DOCUMENTO_ELIMINAR -> eliminarDocumento(payload);
            case AccionesPendientes.TIPO_CLIENTE_CREAR -> enviar(RUTA_CLIENTES, payload);
            case AccionesPendientes.TIPO_CLIENTE_ACTUALIZAR -> actualizarCliente(payload);
            case AccionesPendientes.TIPO_CLIENTE_CONTACTO_AGREGAR -> postSinId(payload, RUTA_CLIENTES, "clienteId", "/contactos");
            case AccionesPendientes.TIPO_PROVEEDOR_CREAR -> enviar(RUTA_PROVEEDORES, payload);
            case AccionesPendientes.TIPO_ARTICULO_CREAR -> enviar(RUTA_INVENTARIO + "/articulos", payload);
            case AccionesPendientes.TIPO_MOVIMIENTO_REGISTRAR -> enviarMovimiento(payload);
            case AccionesPendientes.TIPO_COMPROBANTE_PAGO_REGISTRAR -> registrarPagoComprobante(payload);
            case AccionesPendientes.TIPO_ORDEN_OPERACION_CREAR -> enviar(RUTA_INVENTARIO + "/ordenes-operacion", payload);
            case AccionesPendientes.TIPO_ORDEN_COMPRA_CREAR -> enviar(RUTA_ORDENES_COMPRA, payload);
            case AccionesPendientes.TIPO_ORDEN_COMPRA_RECEPCION ->
                    postSinId(payload, RUTA_ORDENES_COMPRA, "ordenId", "/recepciones");
            case AccionesPendientes.TIPO_ORDEN_COMPRA_CANCELAR ->
                    postSinId(payload, RUTA_ORDENES_COMPRA, "ordenId", "/cancelacion");
            case AccionesPendientes.TIPO_CLIENTE_ESTATUS -> cambiarEstatusCliente(payload);
            case AccionesPendientes.TIPO_ALMACEN_CREAR -> enviar(RUTA_INVENTARIO + "/almacenes", payload);
            case AccionesPendientes.TIPO_PEDIDO_ENTREGAR -> {
                String pedidoId = id(payload, "pedidoId");
                yield gateway.post()
                        .uri(RUTA_PEDIDOS + "/" + pedidoId + "/entrega")
                        .retrieve()
                        .body(String.class);
            }
            case AccionesPendientes.TIPO_INDICADOR_REGISTRAR -> enviar(RUTA_REPORTES + "/indicadores", payload);
            default -> throw new IllegalArgumentException("Tipo de acción no soportado: " + tipo);
        };
    }

    private String cambiarEstatusCliente(String payload) {
        JsonNode datos = leer(payload);
        return gateway.patch()
                .uri(RUTA_CLIENTES + "/" + datos.get("clienteId").asText() + "/estatus")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("estatus", datos.get("estatus").asText()))
                .retrieve()
                .body(String.class);
    }

    private String actualizarCliente(String payload) {
        JsonNode datos = leer(payload);
        return gateway.patch()
                .uri(RUTA_CLIENTES + "/" + datos.get("clienteId").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("razonSocial", datos.get("razonSocial").asText()))
                .retrieve()
                .body(String.class);
    }

    /** Registra el cobro con la clave de idempotencia de la acción, que no forma parte del cuerpo. */
    private String registrarPagoComprobante(String payload) {
        ObjectNode cuerpo = ((ObjectNode) leer(payload)).deepCopy();
        String comprobanteId = cuerpo.remove("comprobanteId").asText();
        String clave = cuerpo.remove("idempotencyKey").asText();
        return gateway.post()
                .uri(RUTA_FACTURACION + "/comprobantes/" + comprobanteId + "/pagos")
                .header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo.toString())
                .retrieve()
                .body(String.class);
    }

    /** Envía el movimiento con la clave de idempotencia de la acción, que no forma parte del cuerpo. */
    private String enviarMovimiento(String payload) {
        ObjectNode cuerpo = ((ObjectNode) leer(payload)).deepCopy();
        String clave = cuerpo.remove("idempotencyKey").asText();
        return gateway.post()
                .uri(RUTA_INVENTARIO + "/movimientos")
                .header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo.toString())
                .retrieve()
                .body(String.class);
    }

    /** Envía al recurso del identificador del payload, sin ese campo en el cuerpo. */
    private String postSinId(String payload, String ruta, String campoId, String sufijo) {
        ObjectNode cuerpo = ((ObjectNode) leer(payload)).deepCopy();
        String id = cuerpo.remove(campoId).asText();
        return gateway.post()
                .uri(ruta + "/" + id + sufijo)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo.toString())
                .retrieve()
                .body(String.class);
    }

    private String eliminarDocumento(String payload) {
        JsonNode datos = leer(payload);
        return gateway.post()
                .uri(RUTA_DOCUMENTOS + "/" + datos.get("documentoId").asText() + "/eliminacion")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("motivo", datos.get("motivo").asText()))
                .retrieve()
                .body(String.class);
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
