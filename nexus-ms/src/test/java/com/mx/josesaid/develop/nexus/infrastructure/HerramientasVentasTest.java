package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HerramientasVentasTest {

    @Test
    void consultarPedidosPasaLosFiltrosYRedactaElCorreo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/pedidos?estatus=CONFIRMADO&canal=WEB"))
                .andRespond(withSuccess("[{\"estatus\":\"CONFIRMADO\",\"contacto\":\"a@b.com\"}]",
                        MediaType.APPLICATION_JSON));

        String texto = new HerramientasVentas(builder.build()).consultarPedidos("confirmado", "web", "");

        assertThat(texto).contains("CONFIRMADO").doesNotContain("a@b.com").contains("[CORREO]");
        servidor.verify();
    }

    @Test
    void resumenVentasPideElMes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://gateway/pedidos-ventas-ms/v1/metricas/ventas?mes=2026-09"))
                .andRespond(withSuccess("{\"mes\":\"2026-09\",\"pedidos\":5}", MediaType.APPLICATION_JSON));

        String texto = new HerramientasVentas(builder.build()).resumenVentas("2026-09");

        assertThat(texto).contains("2026-09");
        servidor.verify();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void prepararPedidoNoLlamaAlGatewayYDejaLaAccionPendiente() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        Map<UUID, AccionPendiente> almacen = new HashMap<>();
        AlmacenAcciones repo = new AlmacenAcciones() {
            @Override
            public void guardar(AccionPendiente a) {
                almacen.put(a.id(), a);
            }

            @Override
            public Optional<AccionPendiente> buscar(UUID id) {
                return Optional.ofNullable(almacen.get(id));
            }
        };
        EjecutorAcciones ejecutor = (tipo, payload) -> {
            throw new AssertionError("preparar no debe ejecutar");
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
        AccionesPendientes acciones = new AccionesPendientes(repo, ejecutor, reloj);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("t").header("alg", "none").subject("u-ventas").build(),
                List.of(new SimpleGrantedAuthority("ROLE_VENTAS"))));

        String texto = new HerramientasVentas(builder.build(), acciones).prepararPedidoBorrador(
                "11111111-1111-1111-1111-111111111111", "web", "mxn", "iva_general",
                "22222222-2222-2222-2222-222222222222", "Silla", 2, "500");

        assertThat(texto).contains("no guardado").contains("accion_id");
        assertThat(almacen).hasSize(1);
        AccionPendiente accion = almacen.values().iterator().next();
        assertThat(accion.usuario()).isEqualTo("u-ventas");
        assertThat(accion.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_BORRADOR);
        assertThat(accion.payload()).contains("\"canal\":\"WEB\"").contains("\"cantidad\":2");
        servidor.verify();
    }
}
