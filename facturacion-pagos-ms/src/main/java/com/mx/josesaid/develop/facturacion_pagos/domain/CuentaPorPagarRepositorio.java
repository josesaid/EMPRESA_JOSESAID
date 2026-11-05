package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de cuentas por pagar. La implementación vive en infraestructura. */
public interface CuentaPorPagarRepositorio {

    CuentaPorPagar guardar(CuentaPorPagar cuenta);

    Optional<CuentaPorPagar> buscarPorId(UUID id);

    Optional<CuentaPorPagar> buscarPorOrdenId(UUID ordenId);
}
