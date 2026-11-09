package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

class HerramientasInventarioEscrituraTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000009";

    private final Map<UUID, AccionPendiente> almacen = new HashMap<>();
    private AccionesPendientes acciones;

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
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-almacen")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ALMACEN"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasInventario herramientas() {
        return new HerramientasInventario(RestClient.builder().baseUrl("http://gateway").build(), acciones);
    }

    @Test
    void sinServicioDeAccionesNoPreparaNada() {
        HerramientasInventario sinAcciones = new HerramientasInventario(RestClient.builder().build());

        assertThat(sinAcciones.crearArticulo("T", "Tubo", "PZA", "", "")).contains("no está disponible");
        assertThat(sinAcciones.registrarMovimientoInventario(ID, ID, "ENTRADA", "1", "", "")).contains("no está disponible");
        assertThat(sinAcciones.crearOrdenOperacion("SERVICIO", "1")).contains("no está disponible");
    }

    @Test
    void articuloSePreparaConLoteYMinimo() {
        String texto = herramientas().crearArticulo("TUB-1", "Tubo cobre", "PZA", "true", "5.5");

        assertThat(texto).contains("no creada").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_ARTICULO_CREAR);
            assertThat(a.payload()).contains("\"loteObligatorio\":true").contains("\"minimo\":5.5");
        });
    }

    @Test
    void articuloValidaSkuNombreUnidadYMinimo() {
        HerramientasInventario h = herramientas();

        assertThat(h.crearArticulo(" ", "Tubo", "PZA", "", "")).contains("SKU es obligatorio");
        assertThat(h.crearArticulo("T", " ", "PZA", "", "")).contains("nombre es obligatorio");
        assertThat(h.crearArticulo("T", "Tubo", "X".repeat(11), "", "")).contains("unidad es obligatoria");
        assertThat(h.crearArticulo("T", "Tubo", "PZA", "", "-1")).contains("mínimo debe");
        assertThat(h.crearArticulo("T", "Tubo", "PZA", "", "abc")).contains("mínimo debe");
        assertThat(almacen).isEmpty();
    }

    @Test
    void movimientoSePreparaConClaveDeIdempotencia() {
        String texto = herramientas().registrarMovimientoInventario(ID, ID, "entrada", "3", "L-7", "OC-12");

        assertThat(texto).contains("no registrado");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_MOVIMIENTO_REGISTRAR);
            assertThat(a.payload()).contains("\"tipo\":\"ENTRADA\"").contains("\"lote\":\"L-7\"")
                    .contains("\"referencia\":\"OC-12\"").contains("idempotencyKey");
        });
    }

    @Test
    void movimientoValidaCadaCampo() {
        HerramientasInventario h = herramientas();

        assertThat(h.registrarMovimientoInventario("no-uuid", ID, "ENTRADA", "1", "", "")).contains("UUID válidos");
        assertThat(h.registrarMovimientoInventario(ID, ID, "PRESTAMO", "1", "", "")).contains("ENTRADA, SALIDA");
        assertThat(h.registrarMovimientoInventario(ID, ID, "ENTRADA", "0", "", "")).contains("cantidad debe");
        assertThat(h.registrarMovimientoInventario(ID, ID, "ENTRADA", "1", "L".repeat(61), "")).contains("lote");
        assertThat(h.registrarMovimientoInventario(ID, ID, "ENTRADA", "1", "", "R".repeat(101))).contains("referencia");
        assertThat(almacen).isEmpty();
    }

    @Test
    void ordenDeOperacionValidaTipoYCantidad() {
        HerramientasInventario h = herramientas();

        assertThat(h.crearOrdenOperacion("MANUAL", "1")).contains("PRODUCCION o SERVICIO");
        assertThat(h.crearOrdenOperacion("PRODUCCION", "-2")).contains("cantidad objetivo debe");
        assertThat(almacen).isEmpty();

        assertThat(h.crearOrdenOperacion("produccion", "12")).contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_ORDEN_OPERACION_CREAR));
    }
}
