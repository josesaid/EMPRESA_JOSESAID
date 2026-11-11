package com.mx.josesaid.develop.contabilidad.domain;

import java.time.YearMonth;
import java.util.Optional;

/** Puerto para guardar y consultar periodos, sin depender de JPA. */
public interface PeriodoRepositorio {
    Periodo guardar(Periodo periodo);
    Optional<Periodo> buscarPorAnioMes(YearMonth anioMes);
}
