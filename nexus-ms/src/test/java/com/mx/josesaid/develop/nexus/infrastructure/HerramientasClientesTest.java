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

class HerramientasClientesTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000006";
    private static final String URL_CLIENTE = "http://gateway/clientes-ms/v1/clientes/" + ID;

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
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-admin")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasClientes herramientas() {
        return new HerramientasClientes(builder.build(), acciones);
    }

    @Test
    void altaDeClienteSePreparaSinEjecutarla() {
        String texto = herramientas().crearCliente("Acme SA", "mogj850315ab3", "Ana", "ana@ejemplo.com", "compras");

        assertThat(texto).contains("no creada").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_CLIENTE_CREAR);
            assertThat(a.payload()).contains("\"rfc\":\"MOGJ850315AB3\"").contains("\"rol\":\"COMPRAS\"");
        });
        servidor.verify();
    }

    @Test
    void altaDeClienteRechazaUnRfcInvalido() {
        String texto = herramientas().crearCliente("Acme", "123", "Ana", "ana@ejemplo.com", "COMPRAS");

        assertThat(texto).isEqualTo("El RFC no tiene un formato válido.");
        assertThat(almacen).isEmpty();
    }

    @Test
    void altaDeClienteRechazaUnRolDeContactoDesconocido() {
        String texto = herramientas().crearCliente("Acme", "MOGJ850315AB3", "Ana", "ana@ejemplo.com", "DIRECCION");

        assertThat(texto).contains("COMPRAS, PAGOS o ASISTENCIA");
        assertThat(almacen).isEmpty();
    }

    @Test
    void cambioDeRazonSocialPreparaLaAccion() {
        servidor.expect(requestTo(URL_CLIENTE))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().actualizarCliente(ID, "Acme SA de CV");

        assertThat(texto).contains("no aplicado").contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_CLIENTE_ACTUALIZAR));
        servidor.verify();
    }

    @Test
    void cambioDeRazonSocialDeClienteInexistenteResponde() {
        servidor.expect(requestTo(URL_CLIENTE)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas().actualizarCliente(ID, "Acme")).isEqualTo("No encontré ese cliente.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void contactoNuevoSePreparaConElPrincipal() {
        servidor.expect(requestTo(URL_CLIENTE))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().agregarContactoCliente(ID, "Luis", "luis@ejemplo.com", "pagos", "true");

        assertThat(texto).contains("accion_id").contains("como principal");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_CLIENTE_CONTACTO_AGREGAR);
            assertThat(a.payload()).contains("\"principal\":true").contains("\"rol\":\"PAGOS\"");
        });
        servidor.verify();
    }

    @Test
    void interaccionesDeUnClienteSeConsultan() {
        servidor.expect(requestTo(URL_CLIENTE + "/interacciones"))
                .andRespond(withSuccess("[{\"tipo\":\"LLAMADA\"}]", MediaType.APPLICATION_JSON));

        assertThat(herramientas().consultarInteraccionesCliente(ID)).contains("LLAMADA");
        servidor.verify();
    }

    @Test
    void interaccionesDeClienteInexistenteResponde() {
        servidor.expect(requestTo(URL_CLIENTE + "/interacciones")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(herramientas().consultarInteraccionesCliente(ID)).isEqualTo("No encontré ese cliente.");
        servidor.verify();
    }

    @Test
    void metricasRechazanUnMesMal() {
        assertThat(herramientas().consultarMetricasConversion("2026/09"))
                .isEqualTo("El mes debe tener formato AAAA-MM, por ejemplo 2026-09.");
    }

    @Test
    void prospectoSeConsulta() {
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/prospectos/" + ID))
                .andRespond(withSuccess("{\"estatus\":\"CALIFICADO\"}", MediaType.APPLICATION_JSON));

        assertThat(herramientas().obtenerProspecto(ID)).contains("CALIFICADO");
        servidor.verify();
    }
}
