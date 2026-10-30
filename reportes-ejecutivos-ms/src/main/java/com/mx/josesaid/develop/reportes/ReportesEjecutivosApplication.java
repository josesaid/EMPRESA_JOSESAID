package com.mx.josesaid.develop.reportes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de reportes-ejecutivos-ms. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class ReportesEjecutivosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportesEjecutivosApplication.class, args);
    }
}
