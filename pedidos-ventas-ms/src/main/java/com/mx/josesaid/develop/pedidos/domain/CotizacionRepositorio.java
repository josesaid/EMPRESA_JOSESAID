package com.mx.josesaid.develop.pedidos.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar cotizaciones, sin depender de JPA. */
public interface CotizacionRepositorio {
    Cotizacion guardar(Cotizacion cotizacion);
    Optional<Cotizacion> buscarPorId(UUID id);
}
