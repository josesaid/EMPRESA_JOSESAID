package com.mx.josesaid.develop.reportes.application;

/** Publica los eventos de dominio de reportes, como reporte-cerrado, para que otros servicios reaccionen. */
public interface PublicadorEventos {
    void publicar(EventoDominio evento);
}
