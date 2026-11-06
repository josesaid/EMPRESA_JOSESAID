package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cifra un campo con AES-256-GCM.
 * <p>
 * Formato en la columna: {@code v<N>:Base64( IV de 12 bytes || texto cifrado+tag )}, donde N es la versión
 * de la clave con la que se cifró. Los valores sin prefijo son de la versión 1 y se siguen leyendo.
 * Un IV aleatorio por valor, así el mismo texto no produce el mismo cifrado.
 */
@Converter
public class CampoCifradoConverter implements AttributeConverter<String, String> {

    private static final int LONGITUD_IV = 12;
    private static final int LONGITUD_TAG_BITS = 128;
    private static final int VERSION_SIN_PREFIJO = 1;
    private static final Pattern PREFIJO_VERSION = Pattern.compile("^v(\\d{1,4}):(.*)$", Pattern.DOTALL);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String texto) {
        if (texto == null) {
            return null;
        }
        int version = ClavesCripto.versionActiva();
        try {
            byte[] iv = new byte[LONGITUD_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, ClavesCripto.cifrado(version), new GCMParameterSpec(LONGITUD_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return "v" + version + ":" + Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar un campo personal", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String columna) {
        if (columna == null) {
            return null;
        }
        int version = VERSION_SIN_PREFIJO;
        String cuerpo = columna;
        Matcher prefijo = PREFIJO_VERSION.matcher(columna);
        if (prefijo.matches()) {
            version = Integer.parseInt(prefijo.group(1));
            cuerpo = prefijo.group(2);
        }
        try {
            byte[] datos = Base64.getDecoder().decode(cuerpo);
            byte[] iv = Arrays.copyOfRange(datos, 0, LONGITUD_IV);
            byte[] cifrado = Arrays.copyOfRange(datos, LONGITUD_IV, datos.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, ClavesCripto.cifrado(version), new GCMParameterSpec(LONGITUD_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar un campo personal", e);
        }
    }
}
