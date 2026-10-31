package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasFinanzasTest {

    private static final String ID = "11111111-2222-3333-4444-555555555555";

    @Test
    void consultarComprobantePideElComprobanteYRedactaElCorreo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/facturacion-pagos-ms/v1/comprobantes/" + ID))
                .andRespond(withSuccess("{\"folio\":\"F-001\",\"estatus\":\"EMITIDO\",\"contacto\":\"a@b.com\"}",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasFinanzas(builder.build()).consultarComprobante(ID);

        assertThat(texto).contains("F-001").contains("EMITIDO").doesNotContain("a@b.com");
        servidor.verify();
    }

    @Test
    void buscarComprobantePorFolioPasaElFolio() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/facturacion-pagos-ms/v1/comprobantes?folio=F-001"))
                .andRespond(withSuccess("{\"folio\":\"F-001\",\"estatus\":\"EMITIDO\"}", MediaType.APPLICATION_JSON));

        String texto = new HerramientasFinanzas(builder.build()).buscarComprobantePorFolio("F-001");

        assertThat(texto).contains("F-001");
        servidor.verify();
    }
}
