package com.mx.josesaid.develop.usuarios_accesos.config;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Carga la clave RSA que firma los tokens. Solo este servicio conoce la clave privada;
 * los demás servicios validan con la clave pública publicada en /.well-known/jwks.json.
 */
public final class ClavesJwt {

    /** Identificador de la clave; viaja en el encabezado "kid" de cada token. */
    public static final String KID = "empresa-usuarios-1";

    private ClavesJwt() {
    }

    /** Falla al arrancar si la clave falta o no es RSA válida: no se firma con una clave por defecto. */
    public static RSAKey desdeBase64(String clavePrivadaBase64) {
        if (clavePrivadaBase64 == null || clavePrivadaBase64.isBlank()) {
            throw new IllegalStateException("usuarios.jwt.clave-privada es obligatoria");
        }
        try {
            byte[] der = Base64.getDecoder().decode(clavePrivadaBase64.replaceAll("\\s", ""));
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privada = (RSAPrivateCrtKey) fabrica.generatePrivate(new PKCS8EncodedKeySpec(der));
            RSAPublicKey publica = (RSAPublicKey) fabrica.generatePublic(
                    new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            return new RSAKey.Builder(publica).privateKey(privada).keyID(KID).build();
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("usuarios.jwt.clave-privada no es una clave RSA válida (PKCS#8 en Base64)", e);
        }
    }
}
