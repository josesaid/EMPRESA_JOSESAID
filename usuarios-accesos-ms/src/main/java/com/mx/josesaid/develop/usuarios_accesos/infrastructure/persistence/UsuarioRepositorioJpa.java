package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto.IndiceCorreo;

import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador que implementa el puerto de dominio con JPA. */
@Repository
public class UsuarioRepositorioJpa implements UsuarioRepositorio {

    private final UsuarioJpaRepository jpa;

    /** Recibe el repositorio JPA de usuarios. */
    public UsuarioRepositorioJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda un usuario y devuelve el guardado. */
    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entidad = jpa.findById(usuario.getId()).orElseGet(UsuarioEntity::new);
        entidad.setId(usuario.getId());
        entidad.setCorreo(usuario.getCorreo());
        entidad.setCorreoIndice(IndiceCorreo.de(usuario.getCorreo()));
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

    /** Busca un usuario por id. */
    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id).map(UsuarioRepositorioJpa::toDominio);
    }

    /** Busca un usuario por correo normalizado, usando el índice de búsqueda. */
    @Override
    public Optional<Usuario> buscarPorCorreo(String correoNormalizado) {
        return jpa.findByCorreoIndice(IndiceCorreo.de(correoNormalizado)).map(UsuarioRepositorioJpa::toDominio);
    }

    /** Indica si ya existe un usuario con ese correo normalizado. */
    @Override
    public boolean existePorCorreo(String correoNormalizado) {
        return jpa.existsByCorreoIndice(IndiceCorreo.de(correoNormalizado));
    }

    /** Lista los usuarios más recientes, con límite. */
    @Override
    public java.util.List<Usuario> listarRecientes(int limite) {
        return jpa.findAllByOrderByCreadoUtcDesc(org.springframework.data.domain.PageRequest.of(0, limite)).stream()
                .map(UsuarioRepositorioJpa::toDominio)
                .toList();
    }

    /** Convierte la fila en el usuario del dominio. */
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
