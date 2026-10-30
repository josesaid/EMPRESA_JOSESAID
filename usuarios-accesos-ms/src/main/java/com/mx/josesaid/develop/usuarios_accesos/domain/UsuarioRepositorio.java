package com.mx.josesaid.develop.usuarios_accesos.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia del agregado Usuario. La implementación vive en infraestructura. */
public interface UsuarioRepositorio {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    /** Recibe el correo ya normalizado. */
    Optional<Usuario> buscarPorCorreo(String correoNormalizado);

    /** Recibe el correo ya normalizado. */
    boolean existePorCorreo(String correoNormalizado);
}
