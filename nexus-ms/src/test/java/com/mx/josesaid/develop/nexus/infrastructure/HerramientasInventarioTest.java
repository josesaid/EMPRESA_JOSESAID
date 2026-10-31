package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasInventarioTest {

    private static final String ARTICULO = "11111111-2222-3333-4444-555555555555";

    @Test
    void consultarExistenciasPasaElArticulo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/existencias?articulo=" + ARTICULO))
                .andRespond(withSuccess("[{\"disponible\":10,\"contacto\":\"a@b.com\"}]", MediaType.APPLICATION_JSON));

        String texto = new HerramientasInventario(builder.build()).consultarExistencias(ARTICULO);

        assertThat(texto).contains("10").doesNotContain("a@b.com");
        servidor.verify();
    }

    @Test
    void consultarTrazabilidadLotePideElLote() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/inventario-operaciones-ms/v1/lotes/L-2026-09/trazabilidad"))
                .andRespond(withSuccess("[{\"tipo\":\"ENTRADA\"}]", MediaType.APPLICATION_JSON));

        String texto = new HerramientasInventario(builder.build()).consultarTrazabilidadLote("L-2026-09");

        assertThat(texto).contains("ENTRADA");
        servidor.verify();
    }
}
