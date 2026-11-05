package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de cuentas por pagar. La implementación vive en infraestructura. */
public interface CuentaPorPagarRepositorio {

    CuentaPorPagar guardar(CuentaPorPagar cuenta);

    Optional<CuentaPorPagar> buscarPorId(UUID id);

    Optional<CuentaPorPagar> buscarPorOrdenId(UUID ordenId);

    /** Pagos más recientes primero, por fecha de pago y luego por registro. */
    List<PagoResumen> listarPagosRecientes(int limite);
}
