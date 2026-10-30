package com.mx.josesaid.develop.documentos.infrastructure.seguridad;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifrado en reposo de archivos con AES-256-GCM. Formato guardado: IV (12 bytes) + texto cifrado con etiqueta.
 * La clave viene de DOCUMENTOS_CIFRADO_CLAVE (base64, 32 bytes). Sin ella el servicio no arranca.
 */
@Component
public class ProtectorArchivos {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec clave;
    private final SecureRandom aleatorio = new SecureRandom();

    public ProtectorArchivos(@Value("${documentos.cifrado.clave:}") String claveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(claveBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("documentos.cifrado.clave no es base64 válido", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("documentos.cifrado.clave debe ser AES-256 (32 bytes en base64)");
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public byte[] cifrar(byte[] claro) {
        byte[] iv = new byte[IV_BYTES];
        aleatorio.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(claro);
            return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el archivo", e);
        }
    }

    public byte[] descifrar(byte[] cifrado) {
        if (cifrado.length <= IV_BYTES) {
            throw new IllegalStateException("Archivo cifrado inválido");
        }
        ByteBuffer buffer = ByteBuffer.wrap(cifrado);
        byte[] iv = new byte[IV_BYTES];
        buffer.get(iv);
        byte[] cuerpo = new byte[buffer.remaining()];
        buffer.get(cuerpo);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(cuerpo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("El archivo no pasó la verificación de integridad", e);
        }
    }
}
