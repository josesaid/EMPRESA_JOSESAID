package com.mx.josesaid.develop.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de ui-ms: la interfaz web que usan los usuarios del sistema. No expone datos por sí misma; los consume del gateway. */
@SpringBootApplication
public class UiMsApplication {

    /** Arranca la aplicación Spring Boot. */
    public static void main(String[] args) {
        SpringApplication.run(UiMsApplication.class, args);
    }
}
