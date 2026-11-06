package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampoCifradoConverterTest {

    private final CampoCifradoConverter convertidor = new CampoCifradoConverter();
    private final SecureRandom aleatorio = new SecureRandom();
    private byte[] clave1;
    private byte[] clave2;

    @BeforeEach
    void configurarClaves() {
        clave1 = bytesAleatorios();
        clave2 = bytesAleatorios();
        ClavesCripto.configurar(Map.of(1, clave1), 1, bytesAleatorios());
    }

    @Test
    void cifraConLaVersionActivaYLeeDeVuelta() {
        String columna = convertidor.convertToDatabaseColumn("juan@ejemplo.mx");

        assertThat(columna).startsWith("v1:");
        assertThat(convertidor.convertToEntityAttribute(columna)).isEqualTo("juan@ejemplo.mx");
    }

    @Test
    void cadenaNulaSeMantieneNula() {
        assertThat(convertidor.convertToDatabaseColumn(null)).isNull();
        assertThat(convertidor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void leeValorAntiguoSinPrefijoComoVersionUno() {
        String antiguo = cifrarSinPrefijo("PEGJ800101AAA", clave1);

        assertThat(convertidor.convertToEntityAttribute(antiguo)).isEqualTo("PEGJ800101AAA");
    }

    @Test
    void escribeConLaVersionActivaNueva() {
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytesAleatorios());

        String columna = convertidor.convertToDatabaseColumn("dato");

        assertThat(columna).startsWith("v2:");
        assertThat(convertidor.convertToEntityAttribute(columna)).isEqualTo("dato");
    }

    @Test
    void leeValorDeVersionAnteriorAunqueLaActivaSeaOtra() {
        String viejo = convertidor.convertToDatabaseColumn("antes de rotar");
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytesAleatorios());

        assertThat(viejo).startsWith("v1:");
        assertThat(convertidor.convertToEntityAttribute(viejo)).isEqualTo("antes de rotar");
    }

    @Test
    void versionNoConfiguradaFalla() {
        String columna = "v9:" + Base64.getEncoder().encodeToString(new byte[40]);

        assertThatThrownBy(() -> convertidor.convertToEntityAttribute(columna))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Versión de clave de cifrado no configurada: 9");
    }

    @Test
    void versionActivaDebeEstarConfigurada() {
        assertThatThrownBy(() -> ClavesCripto.configurar(Map.of(1, clave1), 2, bytesAleatorios()))
                .isInstanceOf(IllegalStateException.class);
    }

    private byte[] bytesAleatorios() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return bytes;
    }

    /** Replica el formato anterior (sin prefijo) para verificar la compatibilidad. */
    private String cifrarSinPrefijo(String texto, byte[] clave) {
        try {
            byte[] iv = new byte[12];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return Base64.getEncoder().encodeToString(salida);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
