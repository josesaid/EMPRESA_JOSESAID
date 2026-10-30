package com.mx.josesaid.develop.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryApplicationTest {

    @Test
    void arrancaElServidorDeRegistro() {
        // Si el contexto no arranca, esta prueba falla.
    }
}
