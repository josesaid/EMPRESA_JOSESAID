package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Almacén de las claves de cifrado del proceso. Lo llena {@code CryptoConfig} al arrancar,
 * porque los convertidores JPA son creados por Hibernate y no por Spring.
 */
public final class ClavesCripto {

    private static volatile SecretKey cifrado;
    private static volatile SecretKey indice;

    private ClavesCripto() {
    }

    /** Configura las claves. Ambas deben tener 32 bytes y ser distintas. */
    public static void configurar(byte[] claveCifrado, byte[] claveIndice) {
        cifrado = new SecretKeySpec(claveCifrado, "AES");
        indice = new SecretKeySpec(claveIndice, "HmacSHA256");
    }

    static SecretKey cifrado() {
        return requerida(cifrado, "cifrado");
    }

    static SecretKey indice() {
        return requerida(indice, "indice");
    }

    private static SecretKey requerida(SecretKey clave, String nombre) {
        if (clave == null) {
            throw new IllegalStateException("Clave de " + nombre + " no configurada");
        }
        return clave;
    }
}
