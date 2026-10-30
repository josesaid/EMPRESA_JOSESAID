package com.mx.josesaid.develop.contabilidad.domain;

import java.util.Optional;
import java.util.UUID;

public interface CuentaRepositorio {
    CuentaContable guardar(CuentaContable cuenta);
    Optional<CuentaContable> buscarPorId(UUID id);
    boolean existePorCodigo(String codigo);
}
