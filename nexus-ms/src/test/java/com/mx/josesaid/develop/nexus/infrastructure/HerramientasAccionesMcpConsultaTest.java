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

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

class HerramientasAccionesMcpConsultaTest {

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
            throw new AssertionError("consultar no debe ejecutar la acción");
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC);
        acciones = new AccionesPendientes(repo, ejecutor, reloj);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void comoUsuario(String sujeto) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(sujeto)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VENTAS"))));
    }

    private HerramientasAccionesMcp herramientas() {
        return new HerramientasAccionesMcp(null, acciones);
    }

    @Test
    void quienLaPreparoVeSuEstado() {
        comoUsuario("u-ventas");
        AccionPendiente accion = acciones.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR,
                "{}", "Crear pedido para cliente X");

        String respuesta = herramientas().consultarEstadoAccion(accion.id().toString());

        assertThat(respuesta).contains("PENDIENTE").contains("Crear pedido para cliente X");
    }

    @Test
    void otroUsuarioNoVeLaAccion() {
        comoUsuario("u-ventas");
        AccionPendiente accion = acciones.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR,
                "{}", "Crear pedido");

        comoUsuario("u-otro");
        String respuesta = herramientas().consultarEstadoAccion(accion.id().toString());

        assertThat(respuesta).isEqualTo("No encontré esa acción.");
    }

    @Test
    void identificadorInvalidoSeRechazaSinLanzar() {
        comoUsuario("u-ventas");

        assertThat(herramientas().consultarEstadoAccion("no-es-uuid"))
                .isEqualTo("El identificador de la acción no es válido.");
    }
}
