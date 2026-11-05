package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasInventarioBajoMinimoTest {

    @Test
    void listarArticulosBajoMinimoPideLaRutaDeInventario() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/articulos/bajo-minimo"))
                .andRespond(withSuccess("[{\"sku\":\"SKU-C\",\"faltante\":20}]", MediaType.APPLICATION_JSON));

        String respuesta = new HerramientasInventario(builder.build()).listarArticulosBajoMinimo();

        servidor.verify();
        assertThat(respuesta).contains("SKU-C");
    }
}
