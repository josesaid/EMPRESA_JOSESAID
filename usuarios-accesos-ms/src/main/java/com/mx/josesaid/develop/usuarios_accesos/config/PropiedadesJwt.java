package com.mx.josesaid.develop.usuarios_accesos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del emisor de tokens, enlazadas desde {@code usuarios.jwt}.
 *
 * @param clavePrivada clave RSA privada en PKCS#8 DER codificada en Base64; nunca se registra en logs
 * @param duracion vida del token de acceso
 * @param emisor   valor del claim {@code iss}
 */
@ConfigurationProperties(prefix = "usuarios.jwt")
public record PropiedadesJwt(String clavePrivada, Duration duracion, String emisor) {
}
