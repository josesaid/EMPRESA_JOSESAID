package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cifra un campo con AES-256-GCM. Formato en la columna: Base64( IV de 12 bytes || texto cifrado+tag ).
 * Un IV aleatorio por valor, así el mismo texto no produce el mismo cifrado.
 */
@Converter
public class CampoCifradoConverter implements AttributeConverter<String, String> {

    private static final int LONGITUD_IV = 12;
    private static final int LONGITUD_TAG_BITS = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[LONGITUD_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, ClavesCripto.cifrado(), new GCMParameterSpec(LONGITUD_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar un campo personal", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String columna) {
        if (columna == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(columna);
            byte[] iv = Arrays.copyOfRange(datos, 0, LONGITUD_IV);
            byte[] cifrado = Arrays.copyOfRange(datos, LONGITUD_IV, datos.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, ClavesCripto.cifrado(), new GCMParameterSpec(LONGITUD_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar un campo personal", e);
        }
    }
}
