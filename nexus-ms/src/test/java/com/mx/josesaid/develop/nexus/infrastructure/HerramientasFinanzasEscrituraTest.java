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

class HerramientasFinanzasEscrituraTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-00000000000a";
    private static final String URL_COMPROBANTE = "http://gateway/facturacion-pagos-ms/v1/comprobantes/" + ID;

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
        acciones = new AccionesPendientes(repo, ejecutor, Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC));
        builder = RestClient.builder().baseUrl("http://gateway");
        servidor = MockRestServiceServer.bindTo(builder).build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-finanzas")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_FINANZAS"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasFinanzas herramientas() {
        return new HerramientasFinanzas(builder.build(), acciones);
    }

    @Test
    void cobroSePreparaSiElComprobanteExiste() {
        servidor.expect(requestTo(URL_COMPROBANTE))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().registrarPagoComprobante(ID, "1500.5", "2026-11-08", "SPEI-99");

        assertThat(texto).contains("no registrado").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_COMPROBANTE_PAGO_REGISTRAR);
            assertThat(a.payload()).contains("\"monto\":1500.5").contains("\"fechaPago\":\"2026-11-08\"")
                    .contains("\"referencia\":\"SPEI-99\"").contains("idempotencyKey");
        });
        servidor.verify();
    }

    @Test
    void cobroDeComprobanteInexistenteResponde() {
        servidor.expect(requestTo(URL_COMPROBANTE)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas().registrarPagoComprobante(ID, "10", "", "")).isEqualTo("No encontré ese comprobante.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cobroValidaMontoFechaYReferencia() {
        HerramientasFinanzas h = herramientas();

        assertThat(h.registrarPagoComprobante("no-uuid", "10", "", "")).contains("identificador del comprobante");
        assertThat(h.registrarPagoComprobante(ID, "0", "", "")).contains("monto debe");
        assertThat(h.registrarPagoComprobante(ID, "1.234", "", "")).contains("monto debe");
        assertThat(h.registrarPagoComprobante(ID, "10", "08/11/2026", "")).contains("AAAA-MM-DD");
        assertThat(h.registrarPagoComprobante(ID, "10", "", "R".repeat(101))).contains("referencia");
        assertThat(almacen).isEmpty();
    }

    @Test
    void cuentaPorPagarSeConsulta() {
        servidor.expect(requestTo("http://gateway/facturacion-pagos-ms/v1/cuentas-por-pagar/" + ID))
                .andRespond(withSuccess("{\"saldo\":\"800.00\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("http://gateway/facturacion-pagos-ms/v1/cuentas-por-pagar/" + ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        HerramientasFinanzas h = herramientas();
        assertThat(h.consultarCuentaPorPagar(ID)).contains("800.00");
        assertThat(h.consultarCuentaPorPagar(ID)).isEqualTo("No encontré esa cuenta por pagar.");
        assertThat(h.consultarCuentaPorPagar("x")).contains("no es válido");
        servidor.verify();
    }
}
