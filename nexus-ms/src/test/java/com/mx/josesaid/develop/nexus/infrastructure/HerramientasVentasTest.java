package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasVentasTest {

    @Test
    void consultarPedidosPasaLosFiltrosYRedactaElCorreo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos?estatus=CONFIRMADO&canal=WEB"))
                .andRespond(withSuccess("[{\"estatus\":\"CONFIRMADO\",\"contacto\":\"a@b.com\"}]",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasVentas(builder.build()).consultarPedidos("confirmado", "web", "");

        assertThat(texto).contains("CONFIRMADO").doesNotContain("a@b.com").contains("[CORREO]");
        servidor.verify();
    }

    @Test
    void resumenVentasPideElMes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/metricas/ventas?mes=2026-09"))
                .andRespond(withSuccess("{\"mes\":\"2026-09\",\"pedidos\":5}", MediaType.APPLICATION_JSON));

        String texto = new HerramientasVentas(builder.build()).resumenVentas("2026-09");

        assertThat(texto).contains("2026-09");
        servidor.verify();
    }
}
