package com.mx.josesaid.develop.usuarios_accesos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del emisor de tokens, enlazadas desde {@code usuarios.jwt}.
 *
 * @param clavePrivada         clave RSA privada en PKCS#8 DER codificada en Base64; nunca se registra en logs
 * @param duracion             vida del token de acceso
 * @param emisor               valor del claim {@code iss}
 * @param kid                  identificador de la clave actual (encabezado "kid")
 * @param clavePublicaAnterior clave pública anterior en X.509 DER Base64; solo durante una rotación
 * @param kidAnterior          identificador de la clave pública anterior
 */
@ConfigurationProperties(prefix = "usuarios.jwt")
public record PropiedadesJwt(String clavePrivada, Duration duracion, String emisor,
                             String kid, String clavePublicaAnterior, String kidAnterior) {
}
