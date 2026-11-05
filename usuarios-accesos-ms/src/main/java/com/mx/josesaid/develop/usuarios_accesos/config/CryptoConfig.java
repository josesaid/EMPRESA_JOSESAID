package com.mx.josesaid.develop.usuarios_accesos.config;

import com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto.ClavesCripto;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Valida las claves al arrancar. Si faltan, no miden 32 bytes o son iguales, el servicio no inicia. */
@Configuration
public class CryptoConfig {

    private static final int LONGITUD_CLAVE = 32;

    public CryptoConfig(@Value("${USUARIOS_CIFRADO_CLAVE:}") String claveCifrado,
                        @Value("${USUARIOS_INDICE_CLAVE:}") String claveIndice) {
        byte[] cifrado = decodificar("USUARIOS_CIFRADO_CLAVE", claveCifrado);
        byte[] indice = decodificar("USUARIOS_INDICE_CLAVE", claveIndice);
        if (Arrays.equals(cifrado, indice)) {
            throw new IllegalStateException("USUARIOS_CIFRADO_CLAVE y USUARIOS_INDICE_CLAVE deben ser distintas");
        }
        ClavesCripto.configurar(cifrado, indice);
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
