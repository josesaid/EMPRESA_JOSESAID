package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia del agregado Comprobante. La implementación vive en infraestructura. */
public interface ComprobanteRepositorio {

    Comprobante guardar(Comprobante comprobante);

    Optional<Comprobante> buscarPorId(UUID id);

    Optional<Comprobante> buscarPorFolio(String folio);
}
