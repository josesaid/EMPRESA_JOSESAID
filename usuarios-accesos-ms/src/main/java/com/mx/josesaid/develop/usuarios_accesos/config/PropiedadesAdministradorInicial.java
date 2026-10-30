package com.mx.josesaid.develop.usuarios_accesos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Administrador inicial opcional, enlazado desde {@code usuarios.admin-inicial}.
 *
 * @param correo   correo del administrador; vacío = no crear
 * @param password contraseña inicial; cámbiala tras el primer inicio de sesión
 */
@ConfigurationProperties(prefix = "usuarios.admin-inicial")
public record PropiedadesAdministradorInicial(String correo, String password) {
}
