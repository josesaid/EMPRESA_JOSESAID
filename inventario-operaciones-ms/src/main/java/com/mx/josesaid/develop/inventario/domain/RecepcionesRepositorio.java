package com.mx.josesaid.develop.inventario.domain;

import java.util.UUID;

/** Control de recepciones de compras ya aplicadas. */
public interface RecepcionesRepositorio {

    /** Registra la recepción como aplicada. Devuelve false si ya estaba aplicada. */
    boolean marcarProcesada(UUID eventoId);
}
