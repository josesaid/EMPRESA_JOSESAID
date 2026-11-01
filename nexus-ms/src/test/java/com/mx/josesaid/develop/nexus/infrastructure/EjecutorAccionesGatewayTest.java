package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void tipoDesconocidoNoSeEjecuta() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

        assertThatThrownBy(() -> new EjecutorAccionesGateway(builder.build()).ejecutar("TIPO_RARO", "{}"))
                .isInstanceOf(IllegalArgumentException.class);
        servidor.verify();
    }
}
