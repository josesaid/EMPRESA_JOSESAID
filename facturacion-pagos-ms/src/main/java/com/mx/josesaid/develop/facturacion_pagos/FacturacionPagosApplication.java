package com.mx.josesaid.develop.facturacion_pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de facturacion-pagos-ms. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class FacturacionPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacturacionPagosApplication.class, args);
    }
}
