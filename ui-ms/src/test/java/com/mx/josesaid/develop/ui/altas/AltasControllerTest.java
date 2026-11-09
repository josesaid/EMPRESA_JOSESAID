package com.mx.josesaid.develop.ui.altas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.ui.web.AltasController;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.client.HttpClientErrorException;

class AltasControllerTest {

    private final AltasCliente altas = mock(AltasCliente.class);
    private final AltasController controlador = new AltasController(altas);

    @Test
    void altaDeClienteEnviaContactoPrincipalYRfcEnMayusculas() {
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.crearCliente(" Comercial SA ", "cpr010101ab1", " Ana ", "ana@empresa.mx", "COMPRAS",
                usuario(), modelo);

        ArgumentCaptor<Map<String, Object>> cuerpo = ArgumentCaptor.forClass(Map.class);
        verify(altas).crearCliente(cuerpo.capture());
        assertThat(cuerpo.getValue()).containsEntry("razonSocial", "Comercial SA").containsEntry("rfc", "CPR010101AB1");
        assertThat(cuerpo.getValue().get("contactoPrincipal"))
                .isEqualTo(Map.of("nombre", "Ana", "correo", "ana@empresa.mx", "rol", "COMPRAS"));
        assertThat(modelo.get("mensaje")).isNotNull();
        assertThat(modelo.get("error")).isNull();
    }

    @Test
    void clienteSinRolMuestraMensajeClaro() {
        when(altas.crearCliente(anyMap())).thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null));
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.crearCliente("Comercial SA", "CPR010101AB1", "Ana", "ana@empresa.mx", "COMPRAS", usuario(), modelo);

        assertThat(modelo.get("error")).isEqualTo("Tu rol no puede dar de alta clientes.");
        assertThat(modelo.get("valores")).isNotNull();
    }

    @Test
    void proveedorDuplicadoMuestraErrorYConservaLosDatos() {
        when(altas.crearProveedor(any())).thenThrow(HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", null, null, null));
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.crearProveedor("Proveedor Prueba SA", "prv010101ab1", "ventas@proveedor.mx", usuario(), modelo);

        assertThat(modelo.get("error")).isEqualTo("Ya existe un registro con ese RFC o correo. No se creó nada.");
        assertThat(modelo.get("valores")).isEqualTo(Map.of("razonSocial", "Proveedor Prueba SA",
                "rfc", "PRV010101AB1", "correo", "ventas@proveedor.mx"));
    }

    @Test
    void formularioTraeRolesDeContacto() {
        ConcurrentModel modelo = new ConcurrentModel();

        String vista = controlador.formularioCliente(usuario(), modelo);

        assertThat(vista).isEqualTo("altas/cliente");
        assertThat(modelo.get("roles")).isEqualTo(List.of("COMPRAS", "PAGOS", "ASISTENCIA"));
    }

    private static UsernamePasswordAuthenticationToken usuario() {
        return new UsernamePasswordAuthenticationToken("ana@empresa.com", "x", List.of());
    }
}
