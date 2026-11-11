package com.mx.josesaid.develop.inventario.application;

/** Publica eventos de dominio de inventario para que otros servicios reaccionen. */
public interface PublicadorEventos {
    void publicar(EventoDominio evento);
}
