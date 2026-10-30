package com.mx.josesaid.develop.pedidos.domain;

/** No se pudo consultar un servicio del que depende la operación. Se traduce a HTTP 503. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
