package com.mx.josesaid.develop.ui.seguridad;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CierreSesionRemotoTest {

    private static final String BASE = "http://gateway";
    private static final String LOGOUT = BASE + "/usuarios-accesos-ms/v1/auth/logout";

    private MockRestServiceServer servidor;
    private CierreSesionRemoto cierre;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cierre = new CierreSesionRemoto(builder.build());
    }

    private static Authentication sesionCon(String refresh) {
        var auth = new UsernamePasswordAuthenticationToken("contador@empresa.mx", null, List.of());
        auth.setDetails(new SesionTokens("access", refresh, Instant.now().plusSeconds(600)));
        return auth;
    }

    @Test
    void enviaElRefreshTokenAlBackendAlCerrarSesion() {
        servidor.expect(requestTo(LOGOUT)).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"refreshToken\":\"r1\"}"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        cierre.logout(null, null, sesionCon("r1"));
        servidor.verify();
    }

    @Test
    void unFalloDelBackendNoImpideElCierreLocal() {
        servidor.expect(requestTo(LOGOUT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatCode(() -> cierre.logout(null, null, sesionCon("r1"))).doesNotThrowAnyException();
    }
}
