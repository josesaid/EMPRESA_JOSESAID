package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;

class HerramientasUsuariosAltaTest {

    private AccionesPendientes acciones;
    private HerramientasUsuarios herramienta;

    @BeforeEach
    void preparar() {
        acciones = mock(AccionesPendientes.class);
        herramienta = new HerramientasUsuarios(mock(RestClient.class), acciones,
                new CodigoConfirmacion(new byte[32]));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void prepararAltaNoRecibeNiGuardaContrasena() {
        UUID id = UUID.randomUUID();
        when(acciones.preparar(anyString(), anyString(), anyString(), anyString())).thenReturn(
                new AccionPendiente(id, "admin", AccionesPendientes.TIPO_USUARIO_ALTA, "{}", "resumen",
                        AccionPendiente.PENDIENTE, Instant.now(), Instant.now().plusSeconds(600)));

        String respuesta = herramienta.prepararAltaUsuario("ana@empresa.com", "Ana López", "VENTAS, CONTADOR");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(acciones).preparar(eq("admin"), eq(AccionesPendientes.TIPO_USUARIO_ALTA),
                payload.capture(), any());
        assertThat(payload.getValue()).contains("ana@empresa.com").doesNotContain("password");
        assertThat(respuesta).contains(id.toString()).contains("Código de confirmación");
    }

    @Test
    void rechazaCorreoInvalidoSinPreparar() {
        String respuesta = herramienta.prepararAltaUsuario("no-es-correo", "Ana", "VENTAS");

        assertThat(respuesta).contains("correo no es válido");
        verify(acciones, never()).preparar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void rechazaRolFueraDeLaListaSinPreparar() {
        String respuesta = herramienta.prepararAltaUsuario("ana@empresa.com", "Ana", "VENTAS, JEFE_AREA");

        assertThat(respuesta).contains("Roles inválidos");
        verify(acciones, never()).preparar(anyString(), anyString(), anyString(), anyString());
    }
}
