package com.mx.josesaid.develop.nexus.application;

/** La sugerencia no cuadra, usa cuentas fuera del catálogo o no es utilizable. No se prepara ninguna acción. */
public class SugerenciaPolizaInvalidaException extends RuntimeException {

    public SugerenciaPolizaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
