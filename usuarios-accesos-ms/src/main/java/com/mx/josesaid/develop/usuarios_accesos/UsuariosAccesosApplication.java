package com.mx.josesaid.develop.usuarios_accesos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de usuarios-accesos-ms. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class UsuariosAccesosApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsuariosAccesosApplication.class, args);
    }
}
