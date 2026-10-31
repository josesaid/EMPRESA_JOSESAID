package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasRedaccionTest {

    @Test
    void reportesRedactaLaRespuestaAntesDeVolverAlModelo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/reportes-ejecutivos-ms/v1/reportes/2026-10"))
                .andRespond(withSuccess("{\"responsable\":\"XAXX010101000\",\"contacto\":\"a@b.com\"}",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasReportes(builder.build()).consultarReporte("2026-10");

        assertThat(texto).doesNotContain("XAXX010101000", "a@b.com").contains("[RFC]", "[CORREO]");
        servidor.verify();
    }

    @Test
    void clientesDejanElRfcAlModeloYOcultanElTelefono() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes/7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .andRespond(withSuccess("{\"rfc\":\"MOGJ850315AB3\",\"telefono\":\"3312345678\"}",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasClientes(builder.build())
                .obtenerCliente("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        assertThat(texto).contains("MOGJ850315AB3").doesNotContain("3312345678").contains("[TELEFONO]");
        servidor.verify();
    }
}
