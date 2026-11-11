package com.mx.josesaid.develop.documentos.application;

/** Publica los eventos de dominio de documentos para que otros servicios reaccionen. */
public interface PublicadorEventos {
    void publicar(EventoDominio evento);
}
