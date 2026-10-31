package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasComprasTest {

    private static final String ID = "11111111-2222-3333-4444-555555555555";

    @Test
    void consultarProveedorRedactaElCorreo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/compras-proveedores-ms/v1/proveedores/" + ID))
                .andRespond(withSuccess("{\"razonSocial\":\"Acme\",\"correo\":\"a@b.com\",\"estatus\":\"ACTIVO\"}",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasCompras(builder.build()).consultarProveedor(ID);

        assertThat(texto).contains("Acme").doesNotContain("a@b.com").contains("[CORREO]");
        servidor.verify();
    }

    @Test
    void consultarOrdenCompraPideLaOrden() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/compras-proveedores-ms/v1/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"estatus\":\"APROBADA\"}", MediaType.APPLICATION_JSON));

        String texto = new HerramientasCompras(builder.build()).consultarOrdenCompra(ID);

        assertThat(texto).contains("APROBADA");
        servidor.verify();
    }

    @Test
    void buscarProveedoresPasaElTexto() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/compras-proveedores-ms/v1/proveedores?razonSocial=Acme"))
                .andRespond(withSuccess("[{\"razonSocial\":\"Acme SA\",\"correo\":\"a@b.com\"}]",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasCompras(builder.build()).buscarProveedores("Acme");

        assertThat(texto).contains("Acme SA").doesNotContain("a@b.com");
        servidor.verify();
    }
}
