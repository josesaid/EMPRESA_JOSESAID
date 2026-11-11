package com.mx.josesaid.develop.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Aplicación de nexus-ms. Activa las tareas programadas, por ejemplo la limpieza de acciones vencidas. */
@SpringBootApplication
@EnableScheduling
public class NexusApplication {

    /** Arranca la aplicación de Spring Boot. */
    public static void main(String[] args) {
        SpringApplication.run(NexusApplication.class, args);
    }
}
