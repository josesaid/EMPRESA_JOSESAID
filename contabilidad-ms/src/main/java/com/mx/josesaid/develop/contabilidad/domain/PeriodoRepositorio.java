package com.mx.josesaid.develop.contabilidad.domain;

import java.time.YearMonth;
import java.util.Optional;

public interface PeriodoRepositorio {
    Periodo guardar(Periodo periodo);
    Optional<Periodo> buscarPorAnioMes(YearMonth anioMes);
}
