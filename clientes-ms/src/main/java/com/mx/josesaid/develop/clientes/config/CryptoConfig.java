package com.mx.josesaid.develop.clientes.config;

import com.mx.josesaid.develop.clientes.infrastructure.crypto.ClavesCripto;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Valida las claves al arrancar. Si faltan o no miden 32 bytes, la aplicación no inicia. */
@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {

    private static final int LONGITUD_CLAVE = 32;

    public CryptoConfig(CryptoProperties propiedades) {
        ClavesCripto.configurar(
                decodificar("app.crypto.clave-cifrado (CRYPTO_CLAVE_CIFRADO)", propiedades.claveCifrado()),
                decodificar("app.crypto.clave-indice (CRYPTO_CLAVE_INDICE)", propiedades.claveIndice()));
    }

    private static byte[] decodificar(String nombre, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta " + nombre);
        }
        byte[] clave = Base64.getDecoder().decode(valor.trim());
        if (clave.length != LONGITUD_CLAVE) {
            throw new IllegalStateException(nombre + " debe ser de 32 bytes en Base64; recibió " + clave.length);
        }
        return clave;
    }
}
