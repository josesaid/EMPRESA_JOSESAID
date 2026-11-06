package com.mx.josesaid.develop.clientes.config;

import com.mx.josesaid.develop.clientes.infrastructure.crypto.ClavesCripto;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Valida las claves al arrancar. Si faltan o no miden 32 bytes, la aplicación no inicia. */
@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {

    private static final int LONGITUD_CLAVE = 32;
    private static final int VERSION_UNICA = 1;

    public CryptoConfig(CryptoProperties propiedades) {
        Map<Integer, byte[]> claves = clavesCifrado(propiedades);
        int version = propiedades.versionActiva() == null ? VERSION_UNICA : propiedades.versionActiva();
        ClavesCripto.configurar(
                claves,
                version,
                decodificar("app.crypto.clave-indice (CRYPTO_CLAVE_INDICE)", propiedades.claveIndice()));
    }

    private static Map<Integer, byte[]> clavesCifrado(CryptoProperties propiedades) {
        Map<Integer, byte[]> claves = new HashMap<>();
        if (propiedades.clavesCifrado() != null && !propiedades.clavesCifrado().isBlank()) {
            for (String entrada : propiedades.clavesCifrado().split(",")) {
                String[] partes = entrada.trim().split(":", 2);
                if (partes.length != 2) {
                    throw new IllegalStateException("CRYPTO_CLAVES_CIFRADO debe tener el formato N:base64,N:base64");
                }
                int numero = Integer.parseInt(partes[0].trim());
                if (claves.put(numero, decodificar("CRYPTO_CLAVES_CIFRADO versión " + numero, partes[1])) != null) {
                    throw new IllegalStateException("La versión " + numero + " de clave aparece más de una vez");
                }
            }
        } else {
            claves.put(VERSION_UNICA,
                    decodificar("app.crypto.clave-cifrado (CRYPTO_CLAVE_CIFRADO)", propiedades.claveCifrado()));
        }
        return claves;
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
