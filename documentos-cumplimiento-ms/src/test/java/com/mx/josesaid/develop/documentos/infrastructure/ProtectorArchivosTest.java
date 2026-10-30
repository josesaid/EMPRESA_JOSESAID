package com.mx.josesaid.develop.documentos.infrastructure;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mx.josesaid.develop.documentos.infrastructure.seguridad.ProtectorArchivos;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ProtectorArchivosTest {

    private static final String CLAVE = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void cifraYDescifraSinPerderElContenido() {
        ProtectorArchivos p = new ProtectorArchivos(CLAVE);
        byte[] claro = "contrato confidencial".getBytes(UTF_8);
        byte[] cifrado = p.cifrar(claro);
        assertFalse(new String(cifrado, ISO_8859_1).contains("confidencial"));
        assertArrayEquals(claro, p.descifrar(cifrado));
    }

    @Test
    void dosCifradosDelMismoContenidoSonDistintos() {
        ProtectorArchivos p = new ProtectorArchivos(CLAVE);
        byte[] claro = "mismo contenido".getBytes(UTF_8);
        assertFalse(Arrays.equals(p.cifrar(claro), p.cifrar(claro)));
    }

    @Test
    void detectaCualquierManipulacionDelArchivo() {
        ProtectorArchivos p = new ProtectorArchivos(CLAVE);
        byte[] cifrado = p.cifrar("dato".getBytes(UTF_8));
        cifrado[cifrado.length - 1] ^= 1;
        assertThrows(IllegalStateException.class, () -> p.descifrar(cifrado));
    }

    @Test
    void rechazaUnaClaveQueNoEsAes256() {
        assertThrows(IllegalStateException.class, () -> new ProtectorArchivos(""));
    }
}
