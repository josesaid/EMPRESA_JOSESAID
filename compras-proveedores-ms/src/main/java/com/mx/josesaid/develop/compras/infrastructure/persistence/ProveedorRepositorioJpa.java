package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.Proveedor;
import com.mx.josesaid.develop.compras.domain.ProveedorRepositorio;
import com.mx.josesaid.develop.compras.infrastructure.seguridad.ProtectorDatos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProveedorRepositorioJpa implements ProveedorRepositorio {

    private final ProveedorJpaRepository jpa;
    private final ProtectorDatos protector;

    ProveedorRepositorioJpa(ProveedorJpaRepository jpa, ProtectorDatos protector) {
        this.jpa = jpa;
        this.protector = protector;
    }

    @Override
    public Proveedor guardar(Proveedor p) {
        ProveedorEntity e = jpa.findById(p.getId()).orElseGet(() -> {
            ProveedorEntity nueva = new ProveedorEntity();
            nueva.id = p.getId();
            return nueva;
        });
        e.razonSocial = p.getRazonSocial();
        e.rfcCifrado = protector.cifrar(p.getRfc());
        e.rfcIndice = protector.indice(p.getRfc());
        e.correoCifrado = protector.cifrar(p.getCorreo());
        e.estatus = p.getEstatus();
        jpa.save(e);
        return p;
    }

    @Override
    public Optional<Proveedor> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> Proveedor.reconstruir(e.id, e.razonSocial,
                protector.descifrar(e.rfcCifrado), protector.descifrar(e.correoCifrado), e.estatus));
    }

    @Override
    public boolean existePorRfc(String rfc) {
        return jpa.existsByRfcIndice(protector.indice(rfc));
    }
}
