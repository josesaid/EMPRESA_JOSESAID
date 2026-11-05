package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasUsuariosListarTest {

    @Test
    void listarUsuariosPideLaRutaDeUsuarios() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/usuarios-accesos-ms/v1/usuarios"))
                .andRespond(withSuccess("[{\"estatus\":\"ACTIVO\"}]", MediaType.APPLICATION_JSON));

        String respuesta = new HerramientasUsuarios(builder.build()).listarUsuarios();

        servidor.verify();
        assertThat(respuesta).contains("ACTIVO");
    }
}
