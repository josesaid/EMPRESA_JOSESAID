package com.mx.josesaid.develop.ui.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.client.HttpClientErrorException;

import com.mx.josesaid.develop.ui.nexus.DatosAltaDto;
import com.mx.josesaid.develop.ui.nexus.NexusCliente;
import com.mx.josesaid.develop.ui.web.UsuariosAltaController;

class UsuariosAltaControllerTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final DatosAltaDto DATOS = new DatosAltaDto("ana@empresa.com", "Ana López", List.of("VENTAS"));

    private final NexusCliente nexus = mock(NexusCliente.class);
    private final UsuariosCliente usuarios = mock(UsuariosCliente.class);
    private final UsuariosAltaController controlador = new UsuariosAltaController(nexus, usuarios);

    @Test
    void completaAltaEnOrdenVerificarCrearYCompletar() {
        when(nexus.datosAlta(ID)).thenReturn(DATOS);

        String vista = controlador.completar(ID, "12345678", "Temporal-1", admin(), new ConcurrentModel());

        InOrder orden = inOrder(nexus, usuarios);
        orden.verify(nexus).verificarAlta(ID, "12345678");
        orden.verify(usuarios).crear(DATOS, "Temporal-1");
        orden.verify(nexus).completarAlta(ID, "12345678");
        assertThat(vista).isEqualTo("usuarios/alta");
    }

    @Test
    void codigoIncorrectoNoCreaUsuario() {
        when(nexus.datosAlta(ID)).thenReturn(DATOS);
        HttpClientErrorException incorrecto = HttpClientErrorException.create(HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable", null, null, null);
        org.mockito.Mockito.doThrow(incorrecto).when(nexus).verificarAlta(eq(ID), anyString());

        controlador.completar(ID, "00000000", "Temporal-1", admin(), new ConcurrentModel());

        verify(usuarios, never()).crear(any(), anyString());
        verify(nexus, never()).completarAlta(any(), anyString());
    }

    @Test
    void noAdminNoLlegaAVerificar() {
        ConcurrentModel modelo = new ConcurrentModel();
        controlador.completar(ID, "12345678", "Temporal-1",
                new UsernamePasswordAuthenticationToken("ventas", null,
                        List.of(new SimpleGrantedAuthority("ROLE_VENTAS"))), modelo);

        verify(nexus, never()).verificarAlta(any(), anyString());
        verify(usuarios, never()).crear(any(), anyString());
        assertThat(modelo.getAttribute("error")).isNotNull();
    }

    private static UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
