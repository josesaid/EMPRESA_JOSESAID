package com.mx.josesaid.develop.ui.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class UsuariosAuthenticationProviderTest {

    private static final String BASE = "http://gateway";
    private static final String LOGIN = BASE + "/usuarios-accesos-ms/v1/auth/login";
    private static final String CORREO = "contador@empresa.mx";

    private MockRestServiceServer servidor;
    private UsuariosAuthenticationProvider proveedor;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        proveedor = new UsuariosAuthenticationProvider(builder.build(), new BloqueoLogin(), new IpOrigen(""));
    }

    @Test
    void loginCorrectoDaRolesDelJwt() {
        servidor.expect(requestTo(LOGIN)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(respuestaOk(), MediaType.APPLICATION_JSON));

        Authentication resultado = proveedor.authenticate(intento("correcta"));

        assertThat(resultado.getName()).isEqualTo(CORREO);
        assertThat(resultado.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CONTADOR");
        servidor.verify();
    }

    @Test
    void contraseñaIncorrectaLanzaCredencialesInvalidas() {
        servidor.expect(requestTo(LOGIN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> proveedor.authenticate(intento("mala")))
                .isInstanceOf(BadCredentialsException.class);
        servidor.verify();
    }

    @Test
    void cincoFallosBloqueanElCorreoAunqueLaContraseñaSeaCorrecta() {
        servidor.expect(org.springframework.test.web.client.ExpectedCount.times(5), requestTo(LOGIN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> proveedor.authenticate(intento("mala")))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // El sexto intento no debe llegar al gateway: el bloqueo corta antes.
        assertThatThrownBy(() -> proveedor.authenticate(intento("correcta")))
                .isInstanceOf(BloqueoLoginException.class);
        servidor.verify();
    }

    @Test
    void loginCorrectoReiniciaElContadorDeFallos() {
        servidor.expect(org.springframework.test.web.client.ExpectedCount.times(4), requestTo(LOGIN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        servidor.expect(requestTo(LOGIN))
                .andRespond(withSuccess(respuestaOk(), MediaType.APPLICATION_JSON));
        servidor.expect(org.springframework.test.web.client.ExpectedCount.times(4), requestTo(LOGIN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> proveedor.authenticate(intento("mala")))
                    .isInstanceOf(BadCredentialsException.class);
        }
        proveedor.authenticate(intento("correcta"));
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> proveedor.authenticate(intento("mala")))
                    .isInstanceOf(BadCredentialsException.class);
        }
        servidor.verify();
    }

    private static Authentication intento(String password) {
        return new UsernamePasswordAuthenticationToken(CORREO, password);
    }

    private static String respuestaOk() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"roles\":[\"CONTADOR\"]}".getBytes(StandardCharsets.UTF_8));
        String jwt = "eyJhbGciOiJub25lIn0." + payload + ".firma";
        return "{\"accessToken\":\"" + jwt + "\",\"tokenType\":\"Bearer\",\"expiresIn\":900,\"refreshToken\":\"r\"}";
    }
}
