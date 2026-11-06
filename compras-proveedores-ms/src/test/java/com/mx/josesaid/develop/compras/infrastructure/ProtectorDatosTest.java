package com.mx.josesaid.develop.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mx.josesaid.develop.compras.infrastructure.seguridad.ProtectorDatos;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class ProtectorDatosTest {

    private static final String CLAVE = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String CLAVE_2 = "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=";

    private final ProtectorDatos protector = new ProtectorDatos(CLAVE, CLAVE);

    @Test
    void cifradoIdaYVueltaYNoRevelaElClaro() {
        String cifrado = protector.cifrar("AAN010101AB3");
        assertThat(cifrado).doesNotContain("AAN010101AB3");
        assertThat(protector.descifrar(cifrado)).isEqualTo("AAN010101AB3");
    }

    @Test
    void cadaCifradoEsDistintoPorqueUsaIvAleatorio() {
        assertThat(protector.cifrar("mismo")).isNotEqualTo(protector.cifrar("mismo"));
    }

    @Test
    void indiceEsDeterministaParaBuscarPorIgualdad() {
        assertThat(protector.indice("AAN010101AB3")).isEqualTo(protector.indice("AAN010101AB3"));
        assertThat(protector.indice("AAN010101AB3")).isNotEqualTo(protector.indice("AAN010101AB4"));
    }

    @Test
    void cifraConPrefijoDeVersionActiva() {
        assertThat(protector.cifrar("AAN010101AB3")).startsWith("v1:");
    }

    @Test
    void leeValorAntiguoSinPrefijoComoVersionUno() {
        String antiguo = cifrarSinPrefijo("AAN010101AB3", CLAVE);
        assertThat(protector.descifrar(antiguo)).isEqualTo("AAN010101AB3");
    }

    @Test
    void rotaCifrandoConVersionNuevaYLeeLaAnterior() {
        ProtectorDatos rotado = new ProtectorDatos("", "1:" + CLAVE + ",2:" + CLAVE_2, 2, CLAVE);

        String nuevo = rotado.cifrar("nuevo");
        String viejo = protector.cifrar("viejo");

        assertThat(nuevo).startsWith("v2:");
        assertThat(rotado.descifrar(nuevo)).isEqualTo("nuevo");
        assertThat(rotado.descifrar(viejo)).isEqualTo("viejo");
    }

    @Test
    void versionNoConfiguradaFalla() {
        assertThatThrownBy(() -> protector.descifrar("v9:" + Base64.getEncoder().encodeToString(new byte[40])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Versión de clave de cifrado no configurada: 9");
    }

    @Test
    void versionActivaDebeEstarConfigurada() {
        assertThatThrownBy(() -> new ProtectorDatos("", "1:" + CLAVE, 2, CLAVE))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Replica el formato anterior (sin prefijo) para verificar la compatibilidad. */
    private static String cifrarSinPrefijo(String texto, String claveBase64) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(Base64.getDecoder().decode(claveBase64), "AES"), new GCMParameterSpec(128, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
