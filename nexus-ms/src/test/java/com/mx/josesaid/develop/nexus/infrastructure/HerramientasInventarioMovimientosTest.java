package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasInventarioMovimientosTest {

    private static final String ARTICULO = "11111111-2222-3333-4444-555555555555";

    @Test
    void listarMovimientosPideElArticulo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/movimientos?articulo=" + ARTICULO))
                .andRespond(withSuccess("[{\"tipo\":\"ENTRADA\",\"cantidad\":5}]", MediaType.APPLICATION_JSON));

        String respuesta = new HerramientasInventario(builder.build()).listarMovimientosArticulo(ARTICULO);

        servidor.verify();
        assertThat(respuesta).contains("ENTRADA");
    }
}
