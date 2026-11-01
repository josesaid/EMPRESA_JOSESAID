package com.mx.josesaid.develop.nexus.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Código de confirmación de una sola vez para acciones preparadas por MCP. Se deriva con HMAC del
 * identificador de la acción y del usuario que la preparó, así que no requiere guardarlo.
 * Quien prepara la acción recibe el código; quien la confirma debe presentarlo.
 */
public final class CodigoConfirmacion {

    private static final String ALGORITMO = "HmacSHA256";
    private static final int MINIMO_BYTES_SECRETO = 16;
    private static final int LONGITUD = 8;

    private final byte[] secreto;

    public CodigoConfirmacion(byte[] secreto) {
        if (secreto == null || secreto.length < MINIMO_BYTES_SECRETO) {
            throw new IllegalArgumentException("El secreto de confirmación debe tener al menos " + MINIMO_BYTES_SECRETO + " bytes");
        }
        this.secreto = secreto.clone();
    }

    public String generar(UUID accionId, String usuario) {
        byte[] mac = hmac(accionId + "|" + usuario);
        int numero = ((mac[0] & 0x7f) << 24) | ((mac[1] & 0xff) << 16) | ((mac[2] & 0xff) << 8) | (mac[3] & 0xff);
        int limite = (int) Math.pow(10, LONGITUD);
        return String.format("%0" + LONGITUD + "d", numero % limite);
    }

    /** Compara en tiempo constante para no revelar el código por diferencias de tiempo. */
    public boolean coincide(UUID accionId, String usuario, String codigo) {
        if (codigo == null) {
            return false;
        }
        byte[] esperado = generar(accionId, usuario).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, codigo.trim().getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmac(String texto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto, ALGORITMO));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el código de confirmación", e);
        }
    }
}
