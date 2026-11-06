package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Almacén de las claves de cifrado del proceso. Hay una clave AES por versión; se cifra siempre
 * con la versión activa, y se descifra con la versión que indica el prefijo del valor.
 * Lo llena {@code CryptoConfig} al arrancar, porque los convertidores JPA los crea Hibernate.
 */
public final class ClavesCripto {

    private static volatile Map<Integer, SecretKey> cifrados = Map.of();
    private static volatile int versionActiva = 1;
    private static volatile SecretKey indice;

    private ClavesCripto() {
    }

    /** Configura una sola clave de cifrado, como versión 1. Ambas deben tener 32 bytes y ser distintas. */
    public static void configurar(byte[] claveCifrado, byte[] claveIndice) {
        configurar(Map.of(1, claveCifrado), 1, claveIndice);
    }

    /** Configura varias versiones de clave de cifrado y cuál usar para cifrar. */
    public static void configurar(Map<Integer, byte[]> clavesCifrado, int version, byte[] claveIndice) {
        Map<Integer, SecretKey> claves = new java.util.HashMap<>();
        clavesCifrado.forEach((numero, bytes) -> claves.put(numero, new SecretKeySpec(bytes, "AES")));
        if (!claves.containsKey(version)) {
            throw new IllegalStateException("La versión activa de clave " + version + " no está configurada");
        }
        cifrados = Map.copyOf(claves);
        versionActiva = version;
        indice = new SecretKeySpec(claveIndice, "HmacSHA256");
    }

    static int versionActiva() {
        return versionActiva;
    }

    static SecretKey cifrado(int version) {
        SecretKey clave = cifrados.get(version);
        if (clave == null) {
            throw new IllegalStateException("Versión de clave de cifrado no configurada: " + version);
        }
        return clave;
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
