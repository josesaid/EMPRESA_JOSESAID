package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

class EjecutorAccionesGatewayTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000003";

    @Test
    void confirmarPedidoLlamaAlEndpointDeConfirmacion() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID + "/confirmacion"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String respuesta = new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_PEDIDO_CONFIRMAR, "{\"pedidoId\":\"" + ID + "\"}");

        assertThat(respuesta).contains(ID);
        servidor.verify();
    }

    @Test
    void cancelarPedidoEnviaElMotivo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID + "/cancelacion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"motivo\":\"ERROR_CAPTURA\"}"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_PEDIDO_CANCELAR,
                        "{\"pedidoId\":\"" + ID + "\",\"motivo\":\"ERROR_CAPTURA\"}");

        servidor.verify();
    }

    @Test
    void prospectoSeEnviaAClientesMs() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        String payload = "{\"nombre\":\"Ana\",\"correo\":\"ana@ejemplo.com\",\"fuente\":\"CAMPANA\"}";
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/prospectos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(payload))
                .andRespond(withSuccess("{\"id\":\"p1\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_PROSPECTO_BORRADOR, payload);

        servidor.verify();
    }

    @Test
    void eliminarDocumentoEnviaElMotivoAlEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/documentos-cumplimiento-ms/v1/documentos/" + ID + "/eliminacion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"motivo\":\"Contrato vencido\"}"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_DOCUMENTO_ELIMINAR,
                        "{\"documentoId\":\"" + ID + "\",\"motivo\":\"Contrato vencido\"}");

        servidor.verify();
    }

    @Test
    void crearClienteEnviaElAltaAClientesMs() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        String payload = "{\"razonSocial\":\"Acme\",\"rfc\":\"MOGJ850315AB3\","
                + "\"contactoPrincipal\":{\"nombre\":\"Ana\",\"correo\":\"ana@ejemplo.com\",\"rol\":\"COMPRAS\"}}";
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(payload))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_CLIENTE_CREAR, payload);

        servidor.verify();
    }

    @Test
    void actualizarClienteEnviaSoloLaRazonSocial() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes/" + ID))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"razonSocial\":\"Acme SA\"}"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_CLIENTE_ACTUALIZAR,
                        "{\"clienteId\":\"" + ID + "\",\"razonSocial\":\"Acme SA\"}");

        servidor.verify();
    }

    @Test
    void agregarContactoQuitaElClienteIdDelCuerpo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes/" + ID + "/contactos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"nombre\":\"Luis\",\"correo\":\"luis@ejemplo.com\",\"rol\":\"PAGOS\",\"principal\":false}"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("clienteId"))))
                .andRespond(withSuccess("{\"id\":\"c2\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_CLIENTE_CONTACTO_AGREGAR,
                        "{\"clienteId\":\"" + ID + "\",\"nombre\":\"Luis\",\"correo\":\"luis@ejemplo.com\","
                                + "\"rol\":\"PAGOS\",\"principal\":false}");

        servidor.verify();
    }

    @Test
    void recepcionEnviaLasLineasSinElOrdenId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/compras-proveedores-ms/v1/ordenes-compra/" + ID + "/recepciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"lineas\":[{\"articuloSku\":\"TUB-1\",\"cantidad\":5}]}"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("ordenId"))))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_ORDEN_COMPRA_RECEPCION,
                        "{\"ordenId\":\"" + ID + "\",\"lineas\":[{\"articuloSku\":\"TUB-1\",\"cantidad\":5}]}");

        servidor.verify();
    }

    @Test
    void cancelarOrdenEnviaElMotivo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/compras-proveedores-ms/v1/ordenes-compra/" + ID + "/cancelacion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"motivo\":\"Proveedor sin stock\"}"))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_ORDEN_COMPRA_CANCELAR,
                        "{\"ordenId\":\"" + ID + "\",\"motivo\":\"Proveedor sin stock\"}");

        servidor.verify();
    }

    @Test
    void altaDeProveedorSeEnviaComoEstaEnElPayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        String payload = "{\"razonSocial\":\"Tubos SA\",\"rfc\":\"TUB850315AB3\",\"correo\":\"ventas@tubos.mx\"}";
        servidor.expect(requestTo("http://gateway/compras-proveedores-ms/v1/proveedores"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(payload))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_PROVEEDOR_CREAR, payload);

        servidor.verify();
    }

    @Test
    void movimientoEnviaLaClaveDeIdempotenciaComoCabecera() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/movimientos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "clave-accion-1"))
                .andExpect(content().json("{\"tipo\":\"ENTRADA\",\"cantidad\":3}"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("idempotencyKey"))))
                .andRespond(withSuccess("{\"id\":\"m1\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_MOVIMIENTO_REGISTRAR,
                        "{\"articuloId\":\"" + ID + "\",\"tipo\":\"ENTRADA\",\"cantidad\":3,\"idempotencyKey\":\"clave-accion-1\"}");

        servidor.verify();
    }

    @Test
    void articuloYOrdenDeOperacionSeEnvianComoEstanEnElPayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/articulos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"sku\":\"TUB-1\",\"loteObligatorio\":true}"))
                .andRespond(withSuccess("{\"id\":\"a1\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/ordenes-operacion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"tipo\":\"SERVICIO\",\"cantidadObjetivo\":10}"))
                .andRespond(withSuccess("{\"id\":\"o1\"}", MediaType.APPLICATION_JSON));

        EjecutorAccionesGateway ejecutor = new EjecutorAccionesGateway(builder.build());
        ejecutor.ejecutar(AccionesPendientes.TIPO_ARTICULO_CREAR, "{\"sku\":\"TUB-1\",\"loteObligatorio\":true}");
        ejecutor.ejecutar(AccionesPendientes.TIPO_ORDEN_OPERACION_CREAR, "{\"tipo\":\"SERVICIO\",\"cantidadObjetivo\":10}");

        servidor.verify();
    }

    @Test
    void pagoDeComprobanteEnviaLaClaveYElMontoSinElId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/facturacion-pagos-ms/v1/comprobantes/" + ID + "/pagos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "clave-pago-1"))
                .andExpect(content().json("{\"monto\":1500.00,\"fechaPago\":\"2026-11-08\"}"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("comprobanteId"))))
                .andRespond(withSuccess("{\"id\":\"p1\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_COMPROBANTE_PAGO_REGISTRAR,
                        "{\"comprobanteId\":\"" + ID + "\",\"monto\":1500.00,\"fechaPago\":\"2026-11-08\","
                                + "\"idempotencyKey\":\"clave-pago-1\"}");

        servidor.verify();
    }

    @Test
    void tipoDesconocidoNoSeEjecuta() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

        assertThatThrownBy(() -> new EjecutorAccionesGateway(builder.build()).ejecutar("TIPO_RARO", "{}"))
                .isInstanceOf(IllegalArgumentException.class);
        servidor.verify();
    }

    @Test
    void accionesDeCotizacionLlamanASuEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        String base = "http://gateway/pedidos-ventas-ms/v1/cotizaciones/" + ID;
        servidor.expect(requestTo(base + "/envio")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(base + "/aceptacion")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(base + "/pedido")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        EjecutorAccionesGateway ejecutor = new EjecutorAccionesGateway(builder.build());
        String payload = "{\"cotizacionId\":\"" + ID + "\"}";

        ejecutor.ejecutar(AccionesPendientes.TIPO_COTIZACION_ENVIAR, payload);
        ejecutor.ejecutar(AccionesPendientes.TIPO_COTIZACION_ACEPTAR, payload);
        ejecutor.ejecutar(AccionesPendientes.TIPO_COTIZACION_A_PEDIDO, payload);

        servidor.verify();
    }

    @Test
    void reemplazarLineasEnviaElPutSinElIdentificadorEnElCuerpo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID + "/lineas"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"lineas\":[{\"cantidad\":3}]}"))
                .andExpect(content().string(not(containsString("pedidoId"))))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_PEDIDO_REEMPLAZAR_LINEAS,
                "{\"pedidoId\":\"" + ID + "\",\"lineas\":[{\"cantidad\":3}]}");

        servidor.verify();
    }

    @Test
    void devolucionLlamaAlEndpointDeDevolucion() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID + "/devolucion"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build())
                .ejecutar(AccionesPendientes.TIPO_PEDIDO_DEVOLVER, "{\"pedidoId\":\"" + ID + "\"}");

        servidor.verify();
    }

    @Test
    void cambioDeEstatusDeProspectoUsaPatch() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/prospectos/" + ID + "/estatus"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"estatus\":\"CALIFICADO\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_PROSPECTO_ESTATUS,
                "{\"prospectoId\":\"" + ID + "\",\"estatus\":\"CALIFICADO\"}");

        servidor.verify();
    }

    @Test
    void conversionDeProspectoLlamaAlEndpointConElContacto() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/prospectos/" + ID + "/conversion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"razonSocial\":\"Acme\",\"contactoPrincipal\":{\"rol\":\"PAGOS\"}}"))
                .andExpect(content().string(not(containsString("prospectoId"))))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_PROSPECTO_CONVERTIR,
                "{\"prospectoId\":\"" + ID + "\",\"razonSocial\":\"Acme\",\"rfc\":\"ACM010101AB1\","
                        + "\"contactoPrincipal\":{\"nombre\":\"Ana\",\"correo\":\"ana@acme.mx\",\"rol\":\"PAGOS\"}}");

        servidor.verify();
    }

    @Test
    void interaccionLlamaAlEndpointDelCliente() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes/" + ID + "/interacciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"tipo\":\"VISITA\",\"resumen\":\"Demo\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_INTERACCION_REGISTRAR,
                "{\"clienteId\":\"" + ID + "\",\"tipo\":\"VISITA\",\"resumen\":\"Demo\"}");

        servidor.verify();
    }

    @Test
    void cotizacionNuevaSeEnviaComoCuerpoAVentas() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/cotizaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"canal\":\"WEB\",\"moneda\":\"MXN\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_COTIZACION_CREAR,
                "{\"clienteId\":\"" + ID + "\",\"canal\":\"WEB\",\"moneda\":\"MXN\"}");

        servidor.verify();
    }
}
