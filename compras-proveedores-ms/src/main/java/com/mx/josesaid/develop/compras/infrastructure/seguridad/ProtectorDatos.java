package com.mx.josesaid.develop.compras.infrastructure.seguridad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra datos sensibles con AES-256-GCM y genera un índice HMAC-SHA256 para buscar sin descifrar.
 * <p>
 * Formato en la columna: {@code v<N>:Base64(IV || cifrado+tag)}, donde N es la versión de la clave con la
 * que se cifró. Los valores sin prefijo son de la versión 1 y se siguen leyendo.
 * Las claves vienen de variables de entorno; sin ellas el servicio no arranca.
 */
@Component
public class ProtectorDatos {

    private static final int TAMANO_IV = 12;
    private static final int TAMANO_TAG_BITS = 128;
    private static final int VERSION_UNICA = 1;
    private static final Pattern PREFIJO_VERSION = Pattern.compile("^v(\\d{1,4}):(.*)$", Pattern.DOTALL);

    private final Map<Integer, SecretKeySpec> clavesCifrado;
    private final int versionActiva;
    private final SecretKeySpec claveIndice;
    private final SecureRandom aleatorio = new SecureRandom();

    /** Una sola clave de cifrado, como versión 1. Se usa en pruebas y como constructor simple. */
    public ProtectorDatos(String claveCifradoBase64, String claveIndiceBase64) {
        this(claveCifradoBase64, "", VERSION_UNICA, claveIndiceBase64);
    }

    @Autowired
    public ProtectorDatos(@Value("${compras.cifrado.clave:}") String claveCifradoBase64,
                          @Value("${compras.cifrado.claves:}") String clavesCifradoBase64,
                          @Value("${compras.cifrado.version-activa:1}") int versionActiva,
                          @Value("${compras.cifrado.indice-clave:}") String claveIndiceBase64) {
        this.clavesCifrado = cargarClavesCifrado(claveCifradoBase64, clavesCifradoBase64);
        if (!clavesCifrado.containsKey(versionActiva)) {
            throw new IllegalStateException("La versión activa de clave de cifrado " + versionActiva + " no está configurada");
        }
        this.versionActiva = versionActiva;
        byte[] indice = decodificar(claveIndiceBase64);
        if (indice.length < 32) {
            throw new IllegalStateException("compras.cifrado.indice-clave debe tener al menos 32 bytes en base64");
        }
        this.claveIndice = new SecretKeySpec(indice, "HmacSHA256");
    }

    private static Map<Integer, SecretKeySpec> cargarClavesCifrado(String claveUnica, String lista) {
        Map<Integer, SecretKeySpec> claves = new HashMap<>();
        if (lista != null && !lista.isBlank()) {
            for (String entrada : lista.split(",")) {
                String[] partes = entrada.trim().split(":", 2);
                if (partes.length != 2) {
                    throw new IllegalStateException("compras.cifrado.claves debe tener el formato N:base64,N:base64");
                }
                int numero = Integer.parseInt(partes[0].trim());
                if (claves.put(numero, claveAes(decodificar(partes[1]))) != null) {
                    throw new IllegalStateException("La versión " + numero + " de clave aparece más de una vez");
                }
            }
        } else {
            claves.put(VERSION_UNICA, claveAes(decodificar(claveUnica)));
        }
        return claves;
    }

    private static SecretKeySpec claveAes(byte[] bytes) {
        if (bytes.length != 32) {
            throw new IllegalStateException("La clave de cifrado debe ser AES-256 de 32 bytes en base64");
        }
        return new SecretKeySpec(bytes, "AES");
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
            cipher.init(Cipher.ENCRYPT_MODE, clavesCifrado.get(versionActiva), new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(claro.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return "v" + versionActiva + ":" + Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    public String descifrar(String cifradoBase64) {
        int version = VERSION_UNICA;
        String cuerpo = cifradoBase64;
        Matcher prefijo = PREFIJO_VERSION.matcher(cifradoBase64);
        if (prefijo.matches()) {
            version = Integer.parseInt(prefijo.group(1));
            cuerpo = prefijo.group(2);
        }
        SecretKeySpec clave = clavesCifrado.get(version);
        if (clave == null) {
            throw new IllegalStateException("Versión de clave de cifrado no configurada: " + version);
        }
        try {
            byte[] entrada = Base64.getDecoder().decode(cuerpo);
            byte[] iv = Arrays.copyOfRange(entrada, 0, TAMANO_IV);
            byte[] contenido = Arrays.copyOfRange(entrada, TAMANO_IV, entrada.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            return new String(cipher.doFinal(contenido), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el dato", e);
        }
    }

    /** Versión con la que se cifra lo nuevo. */
    public int versionActiva() {
        return versionActiva;
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
