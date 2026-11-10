package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

class HerramientasNuevasEscriturasTest {

    private static final String CLIENTE = "7d3c1f2e-0000-4000-8000-000000000011";
    private static final String PEDIDO = "7d3c1f2e-0000-4000-8000-000000000012";

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
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-operaciones")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private AccionPendiente unicaAccion() {
        assertThat(almacen).hasSize(1);
        return almacen.values().iterator().next();
    }

    @Test
    void cambioDeEstatusDeClienteSePreparaSinEjecutarlo() {
        servidor.expect(requestTo("http://gateway/clientes-ms/v1/clientes/" + CLIENTE))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        String respuesta = new HerramientasClientes(builder.build(), acciones)
                .cambiarEstatusCliente(CLIENTE, "suspendido");

        assertThat(respuesta).contains("Cambio preparado, no ejecutado.").contains("accion_id");
        assertThat(unicaAccion().tipo()).isEqualTo(AccionesPendientes.TIPO_CLIENTE_ESTATUS);
        assertThat(unicaAccion().payload()).contains("\"estatus\":\"SUSPENDIDO\"");
        servidor.verify();
    }

    @Test
    void estatusDeClienteInvalidoSeRechaza() {
        String respuesta = new HerramientasClientes(builder.build(), acciones)
                .cambiarEstatusCliente(CLIENTE, "PAUSADO");

        assertThat(respuesta).isEqualTo("El estatus debe ser ACTIVO, SUSPENDIDO o BAJA.");
        assertThat(almacen).isEmpty();
    }

    @Test
    void altaDeAlmacenSePreparaSinEjecutarla() {
        String respuesta = new HerramientasInventario(builder.build(), acciones)
                .crearAlmacen("Central", "Zapopan, Jalisco");

        assertThat(respuesta).contains("Alta preparada, no creada.").contains("accion_id");
        assertThat(unicaAccion().tipo()).isEqualTo(AccionesPendientes.TIPO_ALMACEN_CREAR);
    }

    @Test
    void altaDeAlmacenSinNombreSeRechaza() {
        String respuesta = new HerramientasInventario(builder.build(), acciones)
                .crearAlmacen(" ", "Zapopan");

        assertThat(respuesta).isEqualTo("El nombre es obligatorio y de hasta 100 caracteres.");
        assertThat(almacen).isEmpty();
    }

    @Test
    void entregaDePedidoSePreparaSinEjecutarla() {
        String respuesta = new HerramientasVentas(builder.build(), acciones)
                .registrarEntregaPedido(PEDIDO);

        assertThat(respuesta).contains("Entrega preparada, no registrada.").contains("accion_id");
        assertThat(unicaAccion().tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_ENTREGAR);
        assertThat(unicaAccion().payload()).contains(PEDIDO);
    }

    @Test
    void entregaDePedidoConIdentificadorInvalidoSeRechaza() {
        assertThat(new HerramientasVentas(builder.build(), acciones).registrarEntregaPedido("no-es-uuid"))
                .isEqualTo("El identificador del pedido no es válido.");
        assertThat(almacen).isEmpty();
    }

    @Test
    void indicadorSePreparaConAreaEnMayusculasYValorNumerico() {
        String respuesta = new HerramientasReportes(builder.build(), acciones)
                .registrarIndicador("operaciones", "Entregas a tiempo", "2026-10", "12.5",
                        "2026-11-08T00:00:00Z");

        assertThat(respuesta).contains("Registro preparado, no ejecutado.").contains("accion_id");
        AccionPendiente accion = unicaAccion();
        assertThat(accion.tipo()).isEqualTo(AccionesPendientes.TIPO_INDICADOR_REGISTRAR);
        assertThat(accion.payload()).contains("\"area\":\"OPERACIONES\"").contains("\"periodo\":\"2026-10\"");
    }

    @Test
    void indicadorInvalidoSeRechazaEnCadaCampo() {
        HerramientasReportes herramientas = new HerramientasReportes(builder.build(), acciones);

        assertThat(herramientas.registrarIndicador("RRHH", "x", "2026-10", "1", "2026-11-08T00:00:00Z"))
                .startsWith("El área no es válida");
        assertThat(herramientas.registrarIndicador("OPERACIONES", "x", "2026-13", "1", "2026-11-08T00:00:00Z"))
                .startsWith("El periodo debe ser");
        assertThat(herramientas.registrarIndicador("OPERACIONES", "x", "2026-10", "abc", "2026-11-08T00:00:00Z"))
                .isEqualTo("El valor debe ser numérico.");
        assertThat(herramientas.registrarIndicador("OPERACIONES", "x", "2026-10", "1", "ayer"))
                .startsWith("La fecha de actualización");
        assertThat(almacen).isEmpty();
    }
}
