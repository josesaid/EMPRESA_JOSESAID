package com.mx.josesaid.develop.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.client.enabled=false")
class GatewayApplicationTest {

    @Test
    void arrancaElGateway() {
        // Si el contexto no arranca, esta prueba falla.
    }
}
