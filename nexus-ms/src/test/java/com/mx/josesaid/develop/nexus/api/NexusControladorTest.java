package com.mx.josesaid.develop.nexus.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Mapeo de errores. La validación de la solicitud y el control de acceso por rol se prueban en el paso 20. */
class NexusControladorTest {

    @Test
    void errorDeTiempoAgotadoSeMapeaA504() {
        ManejoErrores manejo = new ManejoErrores();
        var problema = manejo.tiempoAgotado(new org.springframework.web.client.ResourceAccessException("timeout"));

        assertThat(problema.getStatus()).isEqualTo(504);
        assertThat(problema.getTitle()).isEqualTo("Gateway Timeout");
    }
}
