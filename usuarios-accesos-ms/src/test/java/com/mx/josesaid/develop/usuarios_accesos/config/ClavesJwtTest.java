package com.mx.josesaid.develop.usuarios_accesos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ClavesJwtTest {

    private static KeyPair par() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    @Test
    void elKidConfiguradoSeUsaYElDefectoEsElAnterior() throws Exception {
        String privada = Base64.getEncoder().encodeToString(par().getPrivate().getEncoded());
        assertThat(ClavesJwt.desdeBase64(privada, "nueva-2026").getKeyID()).isEqualTo("nueva-2026");
        assertThat(ClavesJwt.desdeBase64(privada, null).getKeyID()).isEqualTo(ClavesJwt.KID_POR_DEFECTO);
    }

    @Test
    void laClavePublicaAnteriorConservaSuModuloYSuKid() throws Exception {
        KeyPair vieja = par();
        String publicaVieja = Base64.getEncoder().encodeToString(vieja.getPublic().getEncoded());
        var anterior = ClavesJwt.publicaAnterior(publicaVieja, "vieja-2025", "nueva-2026");
        assertThat(anterior.getKeyID()).isEqualTo("vieja-2025");
        assertThat(anterior.toRSAPublicKey().getModulus()).isEqualTo(((RSAPublicKey) vieja.getPublic()).getModulus());
        assertThat(anterior.isPrivate()).isFalse();
    }

    @Test
    void laClaveAnteriorNoPuedeTenerElMismoKidQueLaActual() throws Exception {
        String publica = Base64.getEncoder().encodeToString(par().getPublic().getEncoded());
        assertThatThrownBy(() -> ClavesJwt.publicaAnterior(publica, "igual", "igual"))
                .isInstanceOf(IllegalStateException.class);
    }
}
