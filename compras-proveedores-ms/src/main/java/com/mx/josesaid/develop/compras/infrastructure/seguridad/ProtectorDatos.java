package com.mx.josesaid.develop.compras.infrastructure.seguridad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra datos sensibles con AES-256-GCM y genera un índice HMAC-SHA256 para buscar sin descifrar.
 * Las claves vienen de variables de entorno; sin ellas el servicio no arranca.
 */
@Component
public class ProtectorDatos {

    private static final int TAMANO_IV = 12;
    private static final int TAMANO_TAG_BITS = 128;

    private final SecretKeySpec claveCifrado;
    private final SecretKeySpec claveIndice;
    private final SecureRandom aleatorio = new SecureRandom();

    public ProtectorDatos(@Value("${compras.cifrado.clave:}") String claveCifradoBase64,
                          @Value("${compras.cifrado.indice-clave:}") String claveIndiceBase64) {
        byte[] cifrado = decodificar(claveCifradoBase64);
        if (cifrado.length != 32) {
            throw new IllegalStateException("compras.cifrado.clave debe ser una clave AES-256 de 32 bytes en base64");
        }
        byte[] indice = decodificar(claveIndiceBase64);
        if (indice.length < 32) {
            throw new IllegalStateException("compras.cifrado.indice-clave debe tener al menos 32 bytes en base64");
        }
        this.claveCifrado = new SecretKeySpec(cifrado, "AES");
        this.claveIndice = new SecretKeySpec(indice, "HmacSHA256");
    }

    private static byte[] decodificar(String base64) {
        if (base64 == null || base64.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(base64.trim());
    }

    public String cifrar(String claro) {
        try {
            byte[] iv = new byte[TAMANO_IV];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, claveCifrado, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(claro.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    public String descifrar(String cifradoBase64) {
        try {
            byte[] entrada = Base64.getDecoder().decode(cifradoBase64);
            byte[] iv = java.util.Arrays.copyOfRange(entrada, 0, TAMANO_IV);
            byte[] cuerpo = java.util.Arrays.copyOfRange(entrada, TAMANO_IV, entrada.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, claveCifrado, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            return new String(cipher.doFinal(cuerpo), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el dato", e);
        }
    }

    /** Índice determinista para búsquedas por igualdad, por ejemplo el RFC. */
    public String indice(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(claveIndice);
            return Base64.getEncoder().encodeToString(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar el índice", e);
        }
    }
}
