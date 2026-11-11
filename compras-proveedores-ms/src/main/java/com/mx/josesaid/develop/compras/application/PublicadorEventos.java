package com.mx.josesaid.develop.compras.application;

/** Publica eventos de dominio de compras para que otros servicios reaccionen. */
public interface PublicadorEventos {
    void publicar(EventoDominio evento);
}
