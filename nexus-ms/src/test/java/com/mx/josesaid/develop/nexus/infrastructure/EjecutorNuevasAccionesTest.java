package com.mx.josesaid.develop.nexus.infrastructure;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

class EjecutorNuevasAccionesTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000021";

    @Test
    void cambioDeEstatusDeClienteHaceElPatchAlEndpointReal() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes/" + ID + "/estatus"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"estatus\":\"BAJA\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_CLIENTE_ESTATUS,
                "{\"clienteId\":\"" + ID + "\",\"estatus\":\"BAJA\"}");

        servidor.verify();
    }

    @Test
    void altaDeAlmacenHaceElPostAInventario() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/almacenes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"nombre\":\"Central\",\"ubicacion\":\"Zapopan\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_ALMACEN_CREAR,
                "{\"nombre\":\"Central\",\"ubicacion\":\"Zapopan\"}");

        servidor.verify();
    }

    @Test
    void entregaDePedidoHaceElPostSinCuerpo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID + "/entrega"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_PEDIDO_ENTREGAR,
                "{\"pedidoId\":\"" + ID + "\"}");

        servidor.verify();
    }

    @Test
    void registroDeIndicadorHaceElPostAReportes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/reportes-ejecutivos-ms/v1/indicadores"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_INDICADOR_REGISTRAR,
                "{\"area\":\"OPERACIONES\",\"nombre\":\"x\",\"periodo\":\"2026-10\",\"valor\":1,"
                        + "\"fuenteActualizadaUtc\":\"2026-11-08T00:00:00Z\"}");

        servidor.verify();
    }
}
