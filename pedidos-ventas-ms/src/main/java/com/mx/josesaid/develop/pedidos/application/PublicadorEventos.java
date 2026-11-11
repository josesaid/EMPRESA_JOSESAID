package com.mx.josesaid.develop.pedidos.application;

/** Publica eventos de dominio de ventas para que otros servicios reaccionen. */
public interface PublicadorEventos {
    void publicar(EventoDominio evento);
}
