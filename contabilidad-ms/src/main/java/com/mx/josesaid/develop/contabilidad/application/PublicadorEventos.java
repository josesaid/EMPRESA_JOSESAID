package com.mx.josesaid.develop.contabilidad.application;

/** Puerto de publicación de eventos. La implementación usa el patrón outbox. */
public interface PublicadorEventos {

    void publicar(EventoDominio evento);
}
