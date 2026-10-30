package com.mx.josesaid.develop.usuarios_accesos.application;

/** Puerto de publicación de eventos. La implementación usa el patrón outbox. */
public interface PublicadorEventos {

    void publicar(EventoDominio evento);
}
