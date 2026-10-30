package com.mx.josesaid.develop.contabilidad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de contabilidad-ms. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class ContabilidadApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContabilidadApplication.class, args);
    }
}
