package com.mx.josesaid.develop.compras.domain;

/** Puerto para guardar las recepciones de mercancía, sin depender de JPA. */
public interface RecepcionRepositorio {
    Recepcion guardar(Recepcion recepcion);
}
