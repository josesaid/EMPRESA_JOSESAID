package com.mx.josesaid.develop.facturacion_pagos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del emisor de tokens, enlazadas desde {@code facturacion.jwt}.
 *
 * @param jwksUri URL del JWKS de usuarios-accesos-ms con la clave pública para validar tokens
 * @param duracion vida del token de acceso
 * @param emisor   valor del claim {@code iss}
 */
@ConfigurationProperties(prefix = "facturacion.jwt")
public record PropiedadesJwt(String jwksUri, Duration duracion, String emisor) {
}
