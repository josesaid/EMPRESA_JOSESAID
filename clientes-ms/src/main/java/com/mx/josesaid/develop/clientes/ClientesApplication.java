package com.mx.josesaid.develop.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de clientes-ms: alta, seguimiento y consentimientos de clientes y prospectos. */
@SpringBootApplication
@EnableScheduling
public class ClientesApplication {

    /** Arranca la aplicación Spring Boot. */
    public static void main(String[] args) {
        SpringApplication.run(ClientesApplication.class, args);
    }
}
