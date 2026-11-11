package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.Proveedor;
import com.mx.josesaid.develop.compras.domain.ProveedorRepositorio;
import com.mx.josesaid.develop.compras.infrastructure.seguridad.ProtectorDatos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implementa el puerto de proveedores con JPA. */
@Component
class ProveedorRepositorioJpa implements ProveedorRepositorio {

    private final ProveedorJpaRepository jpa;
    private final ProtectorDatos protector;

    ProveedorRepositorioJpa(ProveedorJpaRepository jpa, ProtectorDatos protector) {
        this.jpa = jpa;
        this.protector = protector;
    }

    /** Guarda un proveedor y devuelve el guardado. */
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

    /** Busca un proveedor por id. */
    @Override
    public Optional<Proveedor> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> Proveedor.reconstruir(e.id, e.razonSocial,
                protector.descifrar(e.rfcCifrado), protector.descifrar(e.correoCifrado), e.estatus));
    }

    /** Indica si ya existe un proveedor con ese RFC. */
    @Override
    public boolean existePorRfc(String rfc) {
        return jpa.existsByRfcIndice(protector.indice(rfc));
    }

    /** Busca proveedores cuya razón social contiene el texto indicado. */
    @Override
    public List<Proveedor> buscarPorRazonSocial(String texto) {
        return jpa.findTop20ByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(texto).stream()
                .map(e -> Proveedor.reconstruir(e.id, e.razonSocial,
                        protector.descifrar(e.rfcCifrado), protector.descifrar(e.correoCifrado), e.estatus))
                .toList();
    }
}
