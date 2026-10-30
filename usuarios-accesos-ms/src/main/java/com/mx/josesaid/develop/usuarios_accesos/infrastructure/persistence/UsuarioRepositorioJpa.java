package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador que implementa el puerto de dominio con JPA. */
@Repository
public class UsuarioRepositorioJpa implements UsuarioRepositorio {

    private final UsuarioJpaRepository jpa;

    public UsuarioRepositorioJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entidad = jpa.findById(usuario.getId()).orElseGet(UsuarioEntity::new);
        entidad.setId(usuario.getId());
        entidad.setCorreo(usuario.getCorreo());
        entidad.setNombreCompleto(usuario.getNombreCompleto());
        entidad.setPasswordHash(usuario.getPasswordHash());
        entidad.setEstatus(usuario.getEstatus());
        entidad.setIntentosFallidos(usuario.getIntentosFallidos());
        entidad.setBloqueadoHasta(usuario.getBloqueadoHasta());
        entidad.setArea(usuario.getArea());
        entidad.setDebeCambiarPassword(usuario.debeCambiarPassword());
        entidad.getRoles().clear();
        entidad.getRoles().addAll(usuario.getRoles());
        return toDominio(jpa.save(entidad));
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id).map(UsuarioRepositorioJpa::toDominio);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correoNormalizado) {
        return jpa.findByCorreo(correoNormalizado).map(UsuarioRepositorioJpa::toDominio);
    }

    @Override
    public boolean existePorCorreo(String correoNormalizado) {
        return jpa.existsByCorreo(correoNormalizado);
    }

    private static Usuario toDominio(UsuarioEntity e) {
        return Usuario.reconstruir(
                e.getId(),
                e.getCorreo(),
                e.getNombreCompleto(),
                e.getPasswordHash(),
                e.getRoles(),
                e.getEstatus(),
                e.getIntentosFallidos(),
                e.getBloqueadoHasta()).conArea(e.getArea()).conDebeCambiarPassword(e.isDebeCambiarPassword());
    }
}
