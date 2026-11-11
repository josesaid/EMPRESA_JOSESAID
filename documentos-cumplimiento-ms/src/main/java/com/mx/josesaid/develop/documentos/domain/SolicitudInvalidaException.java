package com.mx.josesaid.develop.documentos.domain;

/** Datos de forma inválidos. HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    /** Crea la excepción con el motivo de la solicitud inválida. */
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
