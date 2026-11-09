package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

/** Validaciones y lecturas de HerramientasCompras que no cubre el flujo feliz. */
class HerramientasComprasValidacionTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000008";
    private static final String BASE = "http://gateway/compras-proveedores-ms/v1";

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
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-compras")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_COMPRAS"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasCompras herramientas() {
        return new HerramientasCompras(builder.build(), acciones);
    }

    @Test
    void sinServicioDeAccionesNoPreparaNada() {
        HerramientasCompras sinAcciones = new HerramientasCompras(builder.build());

        assertThat(sinAcciones.crearProveedor("Tubos", "TUB850315AB3", "a@b.mx")).contains("no está disponible");
        assertThat(sinAcciones.crearOrdenCompra(ID, "MXN", "T", "1", "1")).contains("no está disponible");
        assertThat(sinAcciones.registrarRecepcionOrden(ID, "T", "1")).contains("no está disponible");
        assertThat(sinAcciones.cancelarOrdenCompra(ID, "x")).contains("no está disponible");
        assertThat(almacen).isEmpty();
    }

    @Test
    void altaDeProveedorValidaRazonRfcYLongitud() {
        HerramientasCompras h = herramientas();

        assertThat(h.crearProveedor(" ", "TUB850315AB3", "a@b.mx")).contains("razón social");
        assertThat(h.crearProveedor("Tubos", " ", "a@b.mx")).contains("RFC es obligatorio");
        assertThat(h.crearProveedor("Tubos", "X".repeat(14), "a@b.mx")).contains("RFC es obligatorio");
        assertThat(almacen).isEmpty();
    }

    @Test
    void ordenRechazaCadaCampoInvalido() {
        HerramientasCompras h = herramientas();

        assertThat(h.crearOrdenCompra("no-uuid", "MXN", "T", "1", "1")).contains("identificador del proveedor");
        assertThat(h.crearOrdenCompra(ID, " ", "T", "1", "1")).contains("moneda debe");
        assertThat(h.crearOrdenCompra(ID, "MXNN", "T", "1", "1")).contains("moneda debe");
        assertThat(h.crearOrdenCompra(ID, "MXN", " ", "1", "1")).contains("SKU es obligatorio");
        assertThat(h.crearOrdenCompra(ID, "MXN", "T", "abc", "1")).contains("cantidad debe");
        assertThat(h.crearOrdenCompra(ID, "MXN", "T", "1.2345", "1")).contains("cantidad debe");
        assertThat(h.crearOrdenCompra(ID, "MXN", "T", "1", "-1")).contains("precio unitario no es válido");
        assertThat(h.crearOrdenCompra(ID, "MXN", "T", "1", "abc")).contains("precio unitario no es válido");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void recepcionValidaIdentificadorSkuYCantidad() {
        HerramientasCompras h = herramientas();

        assertThat(h.registrarRecepcionOrden("no-uuid", "T", "1")).contains("identificador de la orden");
        assertThat(h.registrarRecepcionOrden(ID, " ", "1")).contains("SKU es obligatorio");
        assertThat(h.registrarRecepcionOrden(ID, "T", "0")).contains("cantidad debe");
        assertThat(almacen).isEmpty();
    }

    @Test
    void recepcionDeOrdenInexistenteResponde() {
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas().registrarRecepcionOrden(ID, "T", "1")).isEqualTo("No encontré esa orden.");
        servidor.verify();
    }

    @Test
    void cancelacionValidaIdentificadorMotivoYExistencia() {
        HerramientasCompras h = herramientas();

        assertThat(h.cancelarOrdenCompra("no-uuid", "Error")).contains("identificador de la orden");
        assertThat(h.cancelarOrdenCompra(ID, " ")).contains("motivo es obligatorio");
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(h.cancelarOrdenCompra(ID, "Error")).isEqualTo("No encontré esa orden.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelacionRechazaUnaOrdenYaCancelada() {
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"estatus\":\"CANCELADA\"}", MediaType.APPLICATION_JSON));

        assertThat(herramientas().cancelarOrdenCompra(ID, "Error")).contains("no puede cancelarse");
        servidor.verify();
    }

    @Test
    void consultasDeProveedorYOrdenDevuelvenElDetalle() {
        servidor.expect(requestTo(BASE + "/proveedores/" + ID))
                .andRespond(withSuccess("{\"razonSocial\":\"Tubos\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"estatus\":\"APROBADA\"}", MediaType.APPLICATION_JSON));

        HerramientasCompras h = herramientas();
        assertThat(h.consultarProveedor(ID)).contains("Tubos");
        assertThat(h.consultarOrdenCompra(ID)).contains("APROBADA");
        servidor.verify();
    }

    @Test
    void listarOrdenesRechazaUnEstatusDesconocidoYFiltraConUnoValido() {
        HerramientasCompras h = herramientas();
        assertThat(h.listarOrdenesCompra("ENVIADA")).startsWith("Estatus no válido");

        servidor.expect(requestTo(containsString("/ordenes-compra?estatus=APROBADA")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(h.listarOrdenesCompra("aprobada")).isEqualTo("[]");
        servidor.verify();
    }

    @Test
    void buscarProveedoresConsultaPorRazonSocial() {
        servidor.expect(requestTo(containsString("/proveedores")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        herramientas().buscarProveedores("Tubos");
        servidor.verify();
    }
}
