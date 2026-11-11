package com.mx.josesaid.develop.contabilidad.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar cuentas contables, sin depender de JPA. */
public interface CuentaRepositorio {
    CuentaContable guardar(CuentaContable cuenta);
    Optional<CuentaContable> buscarPorId(UUID id);
    boolean existePorCodigo(String codigo);
    /** Cuentas activas, ordenadas por código. */
    List<CuentaContable> listarActivas();
}
