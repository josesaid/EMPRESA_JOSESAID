package com.mx.josesaid.develop.ui.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.ui.nexus.NexusCliente;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class AsistenteNexusControllerTest {

    private final NexusCliente nexus = mock(NexusCliente.class);
    private final AsistenteNexusController controlador = new AsistenteNexusController(nexus);

    @Test
    void preguntaVacíaNoLlamaANexus() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        String vista = controlador.preguntar("   ", new MockHttpSession(), usuario(), flash);

        assertThat(vista).isEqualTo("redirect:/asistente");
        assertThat(flash.getFlashAttributes().get("error")).isEqualTo("Escribe una pregunta antes de enviar.");
        verify(nexus, never()).consultar(anyString());
    }

    @Test
    void preguntaValidaGuardaTurnoEnLaSesion() {
        when(nexus.consultar("¿Cuántas existencias tiene SKU-001?")).thenReturn("Hay 100 piezas.");
        MockHttpSession sesion = new MockHttpSession();

        controlador.preguntar("¿Cuántas existencias tiene SKU-001?", sesion, usuario(), new RedirectAttributesModelMap());

        @SuppressWarnings("unchecked")
        List<AsistenteNexusController.Mensaje> historial =
                (List<AsistenteNexusController.Mensaje>) sesion.getAttribute(AsistenteNexusController.CLAVE_SESION);
        assertThat(historial).extracting(AsistenteNexusController.Mensaje::autor).containsExactly("Tú", "Nexus");
        assertThat(historial.get(1).texto()).isEqualTo("Hay 100 piezas.");
    }

    @Test
    void presupuestoAgotadoMuestraMensajeClaro() {
        when(nexus.consultar(anyString())).thenThrow(HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null));
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        controlador.preguntar("hola", new MockHttpSession(), usuario(), flash);

        assertThat(flash.getFlashAttributes().get("error")).isEqualTo(
                "Se agotó tu presupuesto de consultas al asistente por ahora.");
    }

    @Test
    void elHistorialSePuedeGuardarEnLaSesionDeRedis() throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (var salida = new java.io.ObjectOutputStream(bytes)) {
            salida.writeObject(new java.util.ArrayList<>(List.of(new AsistenteNexusController.Mensaje("Tú", "hola"))));
        }
        Object leido;
        try (var entrada = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            leido = entrada.readObject();
        }

        assertThat(leido).isEqualTo(List.of(new AsistenteNexusController.Mensaje("Tú", "hola")));
    }

    @Test
    void limpiarBorraElHistorial() {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(AsistenteNexusController.CLAVE_SESION,
                new java.util.ArrayList<>(List.of(new AsistenteNexusController.Mensaje("Tú", "hola"))));

        controlador.limpiar(sesion);

        assertThat(sesion.getAttribute(AsistenteNexusController.CLAVE_SESION)).isNull();
    }

    @Test
    void pantallaSinHistorialMuestraListaVacia() {
        ConcurrentModel modelo = new ConcurrentModel();

        String vista = controlador.pantalla(new MockHttpSession(), modelo);

        assertThat(vista).isEqualTo("asistente/nexus");
        assertThat((List<?>) modelo.get("mensajes")).isEmpty();
    }

    private static UsernamePasswordAuthenticationToken usuario() {
        return new UsernamePasswordAuthenticationToken("almacen@empresa.mx", "x", List.of());
    }
}
