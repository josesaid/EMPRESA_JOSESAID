package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasFinanzasComprobantesTest {

    @Test
    void listarComprobantesPendientesPideLaRutaDeCobranza() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/facturacion-pagos-ms/v1/comprobantes/pendientes-cobro"))
                .andRespond(withSuccess("[{\"folio\":\"F-1\",\"saldo\":1160.00}]", MediaType.APPLICATION_JSON));

        String respuesta = new HerramientasFinanzas(builder.build()).listarComprobantesPendientesCobro();

        servidor.verify();
        assertThat(respuesta).contains("F-1");
    }
}
