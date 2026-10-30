package com.mx.josesaid.develop.pedidos.domain;

/** DEVOLUCION_TOTAL solo lo registra la devolución; la cancelación manual no puede usarlo. */
public enum MotivoCancelacion { CANCELACION_CLIENTE, ERROR_CAPTURA, DEVOLUCION_TOTAL }
