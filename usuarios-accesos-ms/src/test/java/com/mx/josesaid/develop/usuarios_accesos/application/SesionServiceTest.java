package com.mx.josesaid.develop.usuarios_accesos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.usuarios_accesos.domain.CredencialesInvalidasException;
import com.mx.josesaid.develop.usuarios_accesos.domain.RefreshToken;
import com.mx.josesaid.develop.usuarios_accesos.domain.RefreshTokenRepositorio;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import com.mx.josesaid.develop.usuarios_accesos.infrastructure.security.EmisorTokens;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SesionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

    private final RefreshTokenRepositorio refrescos = mock(RefreshTokenRepositorio.class);
    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final EmisorTokens emisor = mock(EmisorTokens.class);
    private final SesionService sesiones = new SesionService(refrescos, usuarios, emisor,
            Clock.fixed(AHORA, ZoneOffset.UTC));
    private final Usuario ana = Usuario.crear("ana@empresa.mx", "Ana Ruiz", "hash", List.of(Rol.VENTAS));

    @Test
    void iniciarEmiteAccesoYRefrescoNuevo() {
        when(emisor.emitir(ana)).thenReturn("acceso");
        SesionService.Par par = sesiones.iniciar(ana);
        assertThat(par.accessToken()).isEqualTo("acceso");
        assertThat(par.refreshToken()).isNotBlank();
        verify(refrescos).guardar(any(RefreshToken.class));
    }

    @Test
    void refrescarRotaElTokenYConservaLaFamilia() {
        UUID familia = UUID.randomUUID();
        RefreshToken actual = new RefreshToken(UUID.randomUUID(), ana.getId(), familia, "h",
                AHORA.plusSeconds(3600), null, null);
        when(refrescos.buscarPorHash(anyString())).thenReturn(Optional.of(actual));
        when(usuarios.buscarPorId(ana.getId())).thenReturn(Optional.of(ana));
        when(emisor.emitir(ana)).thenReturn("acceso");

        sesiones.refrescar("plano");

        verify(refrescos).guardar(argThat((RefreshToken r) -> r.id().equals(actual.id()) && r.usadoUtc() != null));
        verify(refrescos).guardar(argThat((RefreshToken r) ->
                r.familiaId().equals(familia) && r.usadoUtc() == null));
    }

    @Test
    void reusoDeTokenRotadoRevocaLaFamiliaYRechaza() {
        UUID familia = UUID.randomUUID();
        RefreshToken usado = new RefreshToken(UUID.randomUUID(), ana.getId(), familia, "h",
                AHORA.plusSeconds(3600), AHORA.minusSeconds(60), null);
        when(refrescos.buscarPorHash(anyString())).thenReturn(Optional.of(usado));

        assertThatThrownBy(() -> sesiones.refrescar("plano")).isInstanceOf(CredencialesInvalidasException.class);
        verify(refrescos).revocarFamilia(familia, AHORA);
        verify(emisor, never()).emitir(any());
    }

    @Test
    void tokenVencidoSeRechaza() {
        RefreshToken vencido = new RefreshToken(UUID.randomUUID(), ana.getId(), UUID.randomUUID(), "h",
                AHORA.minusSeconds(1), null, null);
        when(refrescos.buscarPorHash(anyString())).thenReturn(Optional.of(vencido));

        assertThatThrownBy(() -> sesiones.refrescar("plano")).isInstanceOf(CredencialesInvalidasException.class);
        verify(emisor, never()).emitir(any());
    }

    @Test
    void cerrarRevocaLaFamiliaDelToken() {
        UUID familia = UUID.randomUUID();
        RefreshToken t = new RefreshToken(UUID.randomUUID(), ana.getId(), familia, "h",
                AHORA.plusSeconds(3600), null, null);
        when(refrescos.buscarPorHash(anyString())).thenReturn(Optional.of(t));

        sesiones.cerrar("plano");

        verify(refrescos).revocarFamilia(familia, AHORA);
    }

    @Test
    void cerrarTodasRevocaTodasLasSesionesDelUsuario() {
        sesiones.cerrarTodas(ana.getId());
        verify(refrescos).revocarTodasDeUsuario(ana.getId(), AHORA);
    }
}
