package com.mx.josesaid.develop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de gateway-ms. Las rutas se generan desde Eureka; no hay rutas escritas a mano. */
@SpringBootApplication
public class GatewayApplication {

    /** Arranca gateway-ms. Las rutas no se escriben a mano: se generan desde el registro de Eureka. */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
