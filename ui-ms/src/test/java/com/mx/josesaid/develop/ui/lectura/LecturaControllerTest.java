package com.mx.josesaid.develop.ui.lectura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.client.HttpClientErrorException;

class LecturaControllerTest {

    private final LecturaCliente lectura = mock(LecturaCliente.class);
    private final LecturaController controlador = new LecturaController(lectura);

    @Test
    void listadoSinFiltroRequeridoNoLlamaAlGateway() {
        ConcurrentModel modelo = new ConcurrentModel();

        String vista = controlador.proveedores(null, usuario(), modelo);

        assertThat(vista).isEqualTo("lectura/tabla");
        assertThat(modelo.get("ejecutado")).isEqualTo(false);
        verify(lectura, never()).leer(anyString(), any());
    }

    @Test
    void listadoConFiltroArmaColumnasYEnlaces() {
        Map<String, Object> proveedor = new LinkedHashMap<>();
        proveedor.put("id", "abc");
        proveedor.put("razonSocial", "Papelería SA");
        proveedor.put("fechaAlta", "2026-11-02");
        proveedor.put("contactos", List.of());
        when(lectura.leer(eq("/compras-proveedores-ms/v1/proveedores"), any())).thenReturn(List.of(proveedor));
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.proveedores("Papelería", usuario(), modelo);

        assertThat(modelo.get("ejecutado")).isEqualTo(true);
        List<?> filas = (List<?>) modelo.get("filas");
        assertThat(filas).hasSize(1);
        assertThat(((Map<?, ?>) filas.get(0)).get("_enlace")).isEqualTo("/compras/proveedores/abc");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> columnas = (List<Map<String, String>>) modelo.get("columnas");
        assertThat(columnas.stream().map(c -> c.get("etiqueta")).toList())
                .containsExactly("Razon social", "Fecha alta");
    }

    @Test
    void sinPermisoMuestraMensajeClaro() {
        when(lectura.leer(anyString(), any())).thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null));
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.usuarios(usuario(), modelo);

        assertThat(modelo.get("error")).isEqualTo("Tu rol no tiene acceso a esta sección.");
    }

    @Test
    void loteConCaracteresInvalidosNoLlamaAlGateway() {
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.trazabilidad("../admin", usuario(), modelo);

        assertThat(modelo.get("error")).isEqualTo("El lote tiene caracteres no válidos.");
        verify(lectura, never()).leer(anyString(), any());
    }

    @Test
    void periodoMalFormadoEnIndicadoresNoLlamaAlGateway() {
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.indicadores("2026/11", null, usuario(), modelo);

        assertThat(modelo.get("error")).isEqualTo("El periodo debe tener formato AAAA, AAAA-Qn o AAAA-MM.");
        verify(lectura, never()).leer(anyString(), any());
    }

    private static UsernamePasswordAuthenticationToken usuario() {
        return new UsernamePasswordAuthenticationToken("ana@empresa.com", "x", List.of());
    }
}
