package com.mx.josesaid.develop.clientes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Claves de cifrado. {@code claveCifrado} es la clave única previa (versión 1); {@code clavesCifrado} es la
 * lista {@code N:base64,N:base64} de todas las versiones y {@code versionActiva} la que se usa para cifrar.
 * Vienen de CRYPTO_CLAVE_CIFRADO, CRYPTO_CLAVES_CIFRADO, CRYPTO_VERSION_ACTIVA y CRYPTO_CLAVE_INDICE.
  * @param claveCifrado componente de tipo {@code String}.
  * @param clavesCifrado componente de tipo {@code String}.
  * @param versionActiva componente de tipo {@code Integer}.
  * @param claveIndice componente de tipo {@code String}.
 */
@ConfigurationProperties(prefix = "app.crypto")
public record CryptoProperties(String claveCifrado, String clavesCifrado, Integer versionActiva, String claveIndice) {
}
