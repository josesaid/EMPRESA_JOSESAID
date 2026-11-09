package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ArticuloRepositorioJpa implements ArticuloRepositorio {

    private final ArticuloJpaRepository jpa;

    ArticuloRepositorioJpa(ArticuloJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Articulo guardar(Articulo a) {
        ArticuloEntity e = new ArticuloEntity();
        e.id = a.getId();
        e.sku = a.getSku();
        e.nombre = a.getNombre();
        e.unidad = a.getUnidad();
        e.loteObligatorio = a.isLoteObligatorio();
        e.minimo = a.getMinimo();
        jpa.save(e);
        return a;
    }

    @Override
    public Optional<Articulo> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> Articulo.reconstruir(e.id, e.sku, e.nombre, e.unidad, e.loteObligatorio, e.minimo));
    }

    @Override
    public java.util.List<Articulo> listarConMinimo() {
        return jpa.findAll().stream()
                .filter(e -> e.minimo.signum() > 0)
                .map(e -> Articulo.reconstruir(e.id, e.sku, e.nombre, e.unidad, e.loteObligatorio, e.minimo))
                .toList();
    }

    @Override
    public boolean existePorSku(String sku) {
        return jpa.existsBySku(sku);
    }

    @Override
    public Optional<Articulo> buscarPorSku(String sku) {
        return jpa.findBySku(sku).map(e -> Articulo.reconstruir(e.id, e.sku, e.nombre, e.unidad, e.loteObligatorio, e.minimo));
    }
}
