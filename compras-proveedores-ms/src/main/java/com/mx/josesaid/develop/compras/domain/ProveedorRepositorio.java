package com.mx.josesaid.develop.compras.domain;

import java.util.Optional;
import java.util.UUID;

public interface ProveedorRepositorio {
    Proveedor guardar(Proveedor proveedor);
    Optional<Proveedor> buscarPorId(UUID id);
    boolean existePorRfc(String rfc);
}
