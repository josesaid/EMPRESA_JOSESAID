package com.mx.josesaid.develop.documentos.infrastructure.programacion;

import com.mx.josesaid.develop.documentos.application.DocumentosService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara cada día las alertas de vencimiento y las revisiones de retención. Horario configurable. */
@Component
public class TareasDocumentales {

    private final DocumentosService servicio;

    public TareasDocumentales(DocumentosService servicio) {
        this.servicio = servicio;
    }

    @Scheduled(cron = "${documentos.alertas.cron:0 0 7 * * *}")
    public void procesarVencimientos() {
        servicio.procesarVencimientos();
    }
}
