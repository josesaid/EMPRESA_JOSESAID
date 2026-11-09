package com.mx.josesaid.develop.ui.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ConcurrentModel;

class AgilizaIaControllerTest {

    private final AgilizaIaController controlador = new AgilizaIaController();

    @Test
    void sinOpcionMuestraLasOpcionesDelRolSinElegirNinguna() {
        ConcurrentModel modelo = new ConcurrentModel();

        String vista = controlador.agilizaIa(null, usuario("ALMACEN"), modelo);

        assertThat(vista).isEqualTo("agiliza/ia");
        assertThat(modelo.get("seccion")).isEqualTo("agiliza");
        assertThat((List<?>) modelo.get("opciones")).isNotEmpty();
        assertThat(modelo.get("elegida")).isNull();
    }

    @Test
    void opcionDelRolSeEligeYTrae() {
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.agilizaIa("ALMACEN-0", usuario("ALMACEN"), modelo);

        assertThat(modelo.get("elegida")).isNotNull();
    }

    @Test
    void opcionDeOtroRolNoSeEligeAunqueSeaValida() {
        ConcurrentModel modelo = new ConcurrentModel();

        controlador.agilizaIa("COMPRAS-0", usuario("ALMACEN"), modelo);

        assertThat(modelo.get("elegida")).isNull();
    }

    private static UsernamePasswordAuthenticationToken usuario(String rol) {
        return new UsernamePasswordAuthenticationToken("ana@empresa.com", "x",
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }
}
