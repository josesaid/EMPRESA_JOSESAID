package com.mx.josesaid.develop.pedidos.domain;

import java.util.Optional;
import java.util.UUID;

public interface CotizacionRepositorio {
    Cotizacion guardar(Cotizacion cotizacion);
    Optional<Cotizacion> buscarPorId(UUID id);
}
