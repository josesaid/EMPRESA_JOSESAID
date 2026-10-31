package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasContabilidadTest {

    private static final String ID = "11111111-2222-3333-4444-555555555555";

    @Test
    void consultarPolizaRedactaElCorreo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/contabilidad-ms/v1/polizas/" + ID))
                .andRespond(withSuccess("{\"periodo\":\"2026-09\",\"estatus\":\"APLICADA\",\"contacto\":\"a@b.com\"}",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasContabilidad(builder.build()).consultarPoliza(ID);

        assertThat(texto).contains("2026-09").contains("APLICADA").doesNotContain("a@b.com");
        servidor.verify();
    }

    @Test
    void consultarEstadosFinancierosPasaElPeriodo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/contabilidad-ms/v1/estados-financieros?periodo=2026-09"))
                .andRespond(withSuccess("{\"periodo\":\"2026-09\"}", MediaType.APPLICATION_JSON));

        String texto = new HerramientasContabilidad(builder.build()).consultarEstadosFinancieros("2026-09");

        assertThat(texto).contains("2026-09");
        servidor.verify();
    }
}
