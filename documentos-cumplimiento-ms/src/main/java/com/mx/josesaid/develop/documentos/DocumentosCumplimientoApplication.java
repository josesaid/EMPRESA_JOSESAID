package com.mx.josesaid.develop.documentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de documentos-cumplimiento-ms. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class DocumentosCumplimientoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentosCumplimientoApplication.class, args);
    }
}
