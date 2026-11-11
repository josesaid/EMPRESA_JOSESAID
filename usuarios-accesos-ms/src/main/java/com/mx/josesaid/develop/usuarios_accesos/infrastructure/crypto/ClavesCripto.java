package com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto;

import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Claves de cifrado del proceso. Hay una clave AES por versión: se cifra con la versión activa y se
 * descifra con la que indique el prefijo del valor. Las llena CryptoConfig al arrancar, porque los
 * convertidores JPA los crea Hibernate y no Spring.
 */
public final class ClavesCripto {

    private static volatile Map<Integer, SecretKey> cifrados = Map.of();
    private static volatile int versionActiva = 1;
    private static volatile SecretKey indice;

    /** Clase de utilidad; no se instancia. */
    private ClavesCripto() {
    }

    /** Configura una sola clave de cifrado, como versión 1. Ambas deben tener 32 bytes y ser distintas. */
    public static void configurar(byte[] claveCifrado, byte[] claveIndice) {
        configurar(Map.of(1, claveCifrado), 1, claveIndice);
    }

    /** Configura varias versiones de clave de cifrado y cuál usar para cifrar. */
    public static void configurar(Map<Integer, byte[]> clavesCifrado, int version, byte[] claveIndice) {
        Map<Integer, SecretKey> claves = new HashMap<>();
        clavesCifrado.forEach((numero, bytes) -> claves.put(numero, new SecretKeySpec(bytes, "AES")));
        if (!claves.containsKey(version)) {
            throw new IllegalStateException("La versión activa de clave " + version + " no está configurada");
        }
        cifrados = Map.copyOf(claves);
        versionActiva = version;
        indice = new SecretKeySpec(claveIndice, "HmacSHA256");
    }

    /** Devuelve la versión de clave usada para cifrar nuevos valores. */
    static int versionActiva() {
        return versionActiva;
    }

    /** Devuelve la clave AES de una versión. Falla si esa versión no está configurada. */
    static SecretKey cifrado(int version) {
        SecretKey clave = cifrados.get(version);
        if (clave == null) {
            throw new IllegalStateException("Versión de clave de cifrado no configurada: " + version);
        }
        return clave;
    }

    /** Devuelve la clave del índice de búsqueda. */
    static SecretKey indice() {
        return requerida(indice, "indice");
    }

    /** Verifica que la clave requerida esté configurada antes de usarla. */
    private static SecretKey requerida(SecretKey clave, String nombre) {
        if (clave == null) {
            throw new IllegalStateException("La clave de " + nombre + " no está configurada");
        }
        return clave;
    }
}
