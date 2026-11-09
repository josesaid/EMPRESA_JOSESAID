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

class HerramientasComprasEscrituraTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000007";
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
        Clock reloj = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC);
        acciones = new AccionesPendientes(repo, ejecutor, reloj);
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
    void altaDeProveedorSePreparaSinEjecutarla() {
        String texto = herramientas().crearProveedor("Tubos SA", "tub850315ab3", "ventas@tubos.mx");

        assertThat(texto).contains("no creada").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PROVEEDOR_CREAR);
            assertThat(a.payload()).contains("\"rfc\":\"TUB850315AB3\"");
        });
    }

    @Test
    void altaDeProveedorRechazaUnCorreoInvalido() {
        assertThat(herramientas().crearProveedor("Tubos", "TUB850315AB3", "sin-arroba"))
                .isEqualTo("El correo del proveedor no es válido.");
        assertThat(almacen).isEmpty();
    }

    @Test
    void ordenDeCompraVerificaElProveedorYGuardaNumerosNoTexto() {
        servidor.expect(requestTo(BASE + "/proveedores/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().crearOrdenCompra(ID, "mxn", "TUB-1", "5", "120.50");

        assertThat(texto).contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_ORDEN_COMPRA_CREAR);
            assertThat(a.payload()).contains("\"cantidad\":5").contains("\"precioUnitario\":120.50")
                    .contains("\"moneda\":\"MXN\"");
        });
        servidor.verify();
    }

    @Test
    void ordenDeCompraDeProveedorInexistenteResponde() {
        servidor.expect(requestTo(BASE + "/proveedores/" + ID)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas().crearOrdenCompra(ID, "MXN", "TUB-1", "5", "10")).isEqualTo("No encontré ese proveedor.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void ordenDeCompraRechazaCantidadCero() {
        assertThat(herramientas().crearOrdenCompra(ID, "MXN", "TUB-1", "0", "10")).contains("mayor a cero");
        assertThat(almacen).isEmpty();
    }

    @Test
    void recepcionSePreparaSoloSiLaOrdenEstaAprobada() {
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"APROBADA\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().registrarRecepcionOrden(ID, "TUB-1", "2");

        assertThat(texto).contains("no registrada");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_ORDEN_COMPRA_RECEPCION));
        servidor.verify();
    }

    @Test
    void recepcionRechazaUnaOrdenEnBorrador() {
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"BORRADOR\"}", MediaType.APPLICATION_JSON));

        assertThat(herramientas().registrarRecepcionOrden(ID, "TUB-1", "2")).contains("solo una orden aprobada");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelacionRechazaUnaOrdenRecibida() {
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"RECIBIDA\"}", MediaType.APPLICATION_JSON));

        assertThat(herramientas().cancelarOrdenCompra(ID, "Error")).contains("no puede cancelarse");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelacionSePreparaParaUnaOrdenAbierta() {
        servidor.expect(requestTo(BASE + "/ordenes-compra/" + ID))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"APROBADA\"}", MediaType.APPLICATION_JSON));

        assertThat(herramientas().cancelarOrdenCompra(ID, "Proveedor sin stock")).contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_ORDEN_COMPRA_CANCELAR));
        servidor.verify();
    }
}
