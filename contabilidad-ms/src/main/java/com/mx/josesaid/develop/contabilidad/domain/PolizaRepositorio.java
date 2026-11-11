package com.mx.josesaid.develop.contabilidad.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar pólizas, sin depender de JPA. */
public interface PolizaRepositorio {
    Poliza guardar(Poliza poliza);
    Optional<Poliza> buscarPorId(UUID id);
    List<Poliza> buscarPorPeriodo(YearMonth anioMes);
}
