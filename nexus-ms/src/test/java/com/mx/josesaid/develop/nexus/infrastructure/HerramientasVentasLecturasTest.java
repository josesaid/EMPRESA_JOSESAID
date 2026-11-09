package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasVentasLecturasTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000005";
    private static final String BASE = "http://gateway/pedidos-ventas-ms/v1";

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    private HerramientasVentas herramientas() {
        builder = RestClient.builder().baseUrl("http://gateway");
        servidor = MockRestServiceServer.bindTo(builder).build();
        return new HerramientasVentas(builder.build());
    }

    @Test
    void obtenerPedidoDevuelveElDetalle() {
        HerramientasVentas herramientas = herramientas();
        servidor.expect(requestTo(BASE + "/pedidos/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas.obtenerPedido(ID);

        assertThat(texto).contains("CONFIRMADO");
        servidor.verify();
    }

    @Test
    void obtenerPedidoInexistenteResponde() {
        HerramientasVentas herramientas = herramientas();
        servidor.expect(requestTo(BASE + "/pedidos/" + ID)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas.obtenerPedido(ID)).isEqualTo("No encontré ese pedido.");
        servidor.verify();
    }

    @Test
    void obtenerPedidoRechazaUnIdentificadorInvalido() {
        HerramientasVentas herramientas = herramientas();

        assertThat(herramientas.obtenerPedido("no-es-uuid")).isEqualTo("El identificador del pedido no es válido.");
        servidor.verify();
    }

    @Test
    void obtenerCotizacionDevuelveElDetalle() {
        HerramientasVentas herramientas = herramientas();
        servidor.expect(requestTo(BASE + "/cotizaciones/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"ENVIADA\"}", MediaType.APPLICATION_JSON));

        assertThat(herramientas.obtenerCotizacion(ID)).contains("ENVIADA");
        servidor.verify();
    }

    @Test
    void obtenerCotizacionInexistenteResponde() {
        HerramientasVentas herramientas = herramientas();
        servidor.expect(requestTo(BASE + "/cotizaciones/" + ID)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas.obtenerCotizacion(ID)).isEqualTo("No encontré esa cotización.");
        servidor.verify();
    }

    @Test
    void obtenerCotizacionRechazaUnIdentificadorInvalido() {
        HerramientasVentas herramientas = herramientas();

        assertThat(herramientas.obtenerCotizacion("x")).isEqualTo("El identificador de la cotización no es válido.");
        servidor.verify();
    }

    @Test
    void consultarTasasIvaYMonedasDevuelvenElCatalogo() {
        HerramientasVentas herramientas = herramientas();
        servidor.expect(requestTo(BASE + "/catalogos/tasas-iva"))
                .andRespond(withSuccess("[\"IVA_GENERAL\"]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/catalogos/monedas"))
                .andRespond(withSuccess("[\"MXN\"]", MediaType.APPLICATION_JSON));

        assertThat(herramientas.consultarTasasIva()).contains("IVA_GENERAL");
        assertThat(herramientas.consultarMonedas()).contains("MXN");
        servidor.verify();
    }
}
