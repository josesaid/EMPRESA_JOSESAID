package com.mx.josesaid.develop.usuarios_accesos.config;

import com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto.ClavesCripto;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Valida las claves al arrancar. Si faltan, no miden 32 bytes o son iguales, el servicio no inicia. */
@Configuration
public class CryptoConfig {

    private static final int LONGITUD_CLAVE = 32;
    private static final int VERSION_UNICA = 1;

    /** Recibe la clave de cifrado única y la lista de claves por versión, desde la configuración. */
    public CryptoConfig(@Value("${USUARIOS_CIFRADO_CLAVE:}") String claveCifrado,
                        @Value("${USUARIOS_CIFRADO_CLAVES:}") String clavesCifrado,
                        @Value("${USUARIOS_CIFRADO_VERSION_ACTIVA:1}") int versionActiva,
                        @Value("${USUARIOS_INDICE_CLAVE:}") String claveIndice) {
        Map<Integer, byte[]> cifrado = clavesCifrado(claveCifrado, clavesCifrado);
        byte[] indice = decodificar("USUARIOS_INDICE_CLAVE", claveIndice);
        for (byte[] clave : cifrado.values()) {
            if (Arrays.equals(clave, indice)) {
                throw new IllegalStateException("Las claves de cifrado y la de índice deben ser distintas");
            }
        }
        ClavesCripto.configurar(cifrado, versionActiva, indice);
    }

    /** Construye el mapa de claves de cifrado por versión a partir de la configuración. */
    private static Map<Integer, byte[]> clavesCifrado(String claveUnica, String lista) {
        Map<Integer, byte[]> claves = new HashMap<>();
        if (lista != null && !lista.isBlank()) {
            for (String entrada : lista.split(",")) {
                String[] partes = entrada.trim().split(":", 2);
                if (partes.length != 2) {
                    throw new IllegalStateException("USUARIOS_CIFRADO_CLAVES debe tener el formato N:base64,N:base64");
                }
                int numero = Integer.parseInt(partes[0].trim());
                if (claves.put(numero, decodificar("USUARIOS_CIFRADO_CLAVES versión " + numero, partes[1])) != null) {
                    throw new IllegalStateException("La versión " + numero + " de clave aparece más de una vez");
                }
            }
        } else {
            claves.put(VERSION_UNICA, decodificar("USUARIOS_CIFRADO_CLAVE", claveUnica));
        }
        return claves;
    }

    /** Decodifica una clave en Base64 y valida su longitud. */
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
