package com.mx.josesaid.develop.usuarios_accesos.config;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Carga las claves RSA que firman y validan los tokens. Solo este servicio conoce la clave privada;
 * los demás servicios validan con las claves públicas publicadas en /.well-known/jwks.json.
 */
public final class ClavesJwt {

    /** Identificador por defecto de la clave; viaja en el encabezado "kid" de cada token. */
    public static final String KID_POR_DEFECTO = "empresa-usuarios-1";

    private ClavesJwt() {
    }

    /** Devuelve el kid configurado, o el valor por defecto si no se configuró ninguno. */
    public static String kid(String kidConfigurado) {
        return kidConfigurado == null || kidConfigurado.isBlank() ? KID_POR_DEFECTO : kidConfigurado.trim();
    }

    /** Falla al arrancar si la clave falta o no es RSA válida: no se firma con una clave por defecto. */
    public static RSAKey desdeBase64(String clavePrivadaBase64, String kidConfigurado) {
        if (clavePrivadaBase64 == null || clavePrivadaBase64.isBlank()) {
            throw new IllegalStateException("usuarios.jwt.clave-privada es obligatoria");
        }
        try {
            byte[] der = Base64.getDecoder().decode(clavePrivadaBase64.replaceAll("\\s", ""));
            KeyFactory fabrica = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privada = (RSAPrivateCrtKey) fabrica.generatePrivate(new PKCS8EncodedKeySpec(der));
            RSAPublicKey publica = (RSAPublicKey) fabrica.generatePublic(
                    new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent()));
            return new RSAKey.Builder(publica).privateKey(privada).keyID(kid(kidConfigurado)).build();
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("usuarios.jwt.clave-privada no es una clave RSA válida (PKCS#8 en Base64)", e);
        }
    }

    /**
     * Clave pública anterior de una rotación. Sigue validando los tokens emitidos antes del cambio
     * hasta que expiren; después se quita de la configuración.
     */
    public static RSAKey publicaAnterior(String clavePublicaBase64, String kidAnterior, String kidActual) {
        if (clavePublicaBase64 == null || clavePublicaBase64.isBlank()) {
            throw new IllegalStateException("usuarios.jwt.clave-publica-anterior está vacía");
        }
        if (kidAnterior == null || kidAnterior.isBlank()) {
            throw new IllegalStateException("usuarios.jwt.kid-anterior es obligatorio cuando hay clave-publica-anterior");
        }
        if (kidAnterior.trim().equals(kidActual)) {
            throw new IllegalStateException("usuarios.jwt.kid-anterior debe ser distinto de usuarios.jwt.kid");
        }
        try {
            byte[] der = Base64.getDecoder().decode(clavePublicaBase64.replaceAll("\\s", ""));
            RSAPublicKey publica = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
            return new RSAKey.Builder(publica).keyID(kidAnterior.trim()).build();
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("usuarios.jwt.clave-publica-anterior no es una clave pública RSA válida (X.509 en Base64)", e);
        }
    }
}
