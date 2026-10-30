package com.mx.josesaid.develop.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Punto de entrada de pedidos-ventas-ms. */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class PedidosVentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosVentasApplication.class, args);
    }
}
