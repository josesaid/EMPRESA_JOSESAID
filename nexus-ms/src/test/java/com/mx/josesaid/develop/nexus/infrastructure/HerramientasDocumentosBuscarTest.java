package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasDocumentosBuscarTest {

    @Test
    void buscarDocumentosPideElTituloPorQueryParam() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/documentos-cumplimiento-ms/v1/documentos?titulo=contrato"))
                .andRespond(withSuccess("[{\"titulo\":\"Contrato marco\"}]", MediaType.APPLICATION_JSON));

        String respuesta = new HerramientasDocumentos(builder.build()).buscarDocumentos("contrato");

        servidor.verify();
        assertThat(respuesta).contains("Contrato marco");
    }
}
