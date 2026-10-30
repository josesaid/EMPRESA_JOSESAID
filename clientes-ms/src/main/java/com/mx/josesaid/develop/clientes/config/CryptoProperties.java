package com.mx.josesaid.develop.clientes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Claves en Base64 (32 bytes cada una). Vienen de CRYPTO_CLAVE_CIFRADO y CRYPTO_CLAVE_INDICE. */
@ConfigurationProperties(prefix = "app.crypto")
public record CryptoProperties(String claveCifrado, String claveIndice) {
}
