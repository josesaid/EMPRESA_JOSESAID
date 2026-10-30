package com.mx.josesaid.develop.documentos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del emisor de tokens, enlazadas desde {@code documentos.jwt}.
 *
 * @param secreto clave HMAC de al menos 32 caracteres; nunca se registra en logs
 * @param duracion vida del token de acceso
 * @param emisor   valor del claim {@code iss}
 */
@ConfigurationProperties(prefix = "documentos.jwt")
public record PropiedadesJwt(String secreto, Duration duracion, String emisor) {
}
