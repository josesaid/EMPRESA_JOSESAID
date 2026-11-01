package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

class HerramientasAccionesVentasTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000002";
    private static final String URL_PEDIDO = "http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID;

    private final Map<UUID, AccionPendiente> almacen = new HashMap<>();
    private AccionesPendientes acciones;
    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
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
            throw new AssertionError("preparar no debe ejecutar la acción");
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
        acciones = new AccionesPendientes(repo, ejecutor, reloj);
        builder = RestClient.builder().baseUrl("http://gateway");
        servidor = MockRestServiceServer.bindTo(builder).build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-ventas")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VENTAS"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasVentas herramientas() {
        return new HerramientasVentas(builder.build(), acciones);
    }

    @Test
    void prospectoSePreparaSinLlamarAlGateway() {
        String texto = herramientas().crearProspectoBorrador("Ana Prospecto", "ana@ejemplo.com", "", "referido");

        assertThat(texto).contains("no guardado").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PROSPECTO_BORRADOR);
            assertThat(a.payload()).contains("\"fuente\":\"REFERIDO\"");
        });
        servidor.verify();
    }

    @Test
    void prospectoRechazaUnaFuenteDesconocida() {
        String texto = herramientas().crearProspectoBorrador("Ana", "ana@ejemplo.com", "", "radio");

        assertThat(texto).contains("CAMPANA, REFERIDO o EVENTO");
        assertThat(almacen).isEmpty();
    }

    @Test
    void confirmarPedidoSePreparaSiEstaEnBorrador() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"BORRADOR\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().confirmarPedido(ID);

        assertThat(texto).contains("no ejecutada").contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_CONFIRMAR));
        servidor.verify();
    }

    @Test
    void confirmarPedidoRechazaUnPedidoYaConfirmado() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().confirmarPedido(ID);

        assertThat(texto).contains("solo un pedido en borrador puede confirmarse");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void confirmarPedidoInexistenteResponde() {
        servidor.expect(requestTo(URL_PEDIDO)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        String texto = herramientas().confirmarPedido(ID);

        assertThat(texto).isEqualTo("No encontré ese pedido.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelarPedidoRechazaLaDevolucionTotal() {
        String texto = herramientas().cancelarPedido(ID, "DEVOLUCION_TOTAL");

        assertThat(texto).contains("CANCELACION_CLIENTE o ERROR_CAPTURA");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelarPedidoSePreparaSiEstaConfirmado() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().cancelarPedido(ID, "error_captura");

        assertThat(texto).contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_CANCELAR);
            assertThat(a.payload()).contains("\"motivo\":\"ERROR_CAPTURA\"");
        });
        servidor.verify();
    }
}
