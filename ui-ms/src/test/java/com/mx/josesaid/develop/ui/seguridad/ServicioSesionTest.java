package com.mx.josesaid.develop.ui.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class ServicioSesionTest {

    private static final String BASE = "http://gateway";
    private static final String REFRESH = BASE + "/usuarios-accesos-ms/v1/auth/refresh";
    private static final String RESPUESTA_NUEVA = """
            {"accessToken":"nuevo","tokenType":"Bearer","expiresIn":900,"refreshToken":"r2"}
            """;

    private MockRestServiceServer servidor;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valores;
    private SessionRepository<Session> sesiones;
    private ServicioSesion sesion;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        redis = mock(StringRedisTemplate.class);
        valores = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        when(valores.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        sesiones = mock(SessionRepository.class);
        sesion = new ServicioSesion(builder.build(), redis, sesiones);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static SesionTokens conectar(Instant expiraEn) {
        SesionTokens tokens = new SesionTokens("viejo", "r1", expiraEn);
        var auth = new UsernamePasswordAuthenticationToken("contador@empresa.mx", null, List.of());
        auth.setDetails(tokens);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return tokens;
    }

    private static Session sesionGuardada(String access, String refresh) {
        Session guardada = org.mockito.Mockito.mock(Session.class);
        var auth = new UsernamePasswordAuthenticationToken("contador@empresa.mx", null, List.of());
        auth.setDetails(new SesionTokens(access, refresh, Instant.now().plusSeconds(600)));
        when(guardada.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .thenReturn(new SecurityContextImpl(auth));
        return guardada;
    }

    @Test
    void noRenuevaSiElTokenEstaVigente() {
        conectar(Instant.now().plusSeconds(600));
        assertThat(sesion.accessTokenVigente()).isEqualTo("viejo");
        servidor.verify();
    }

    @Test
    void renuevaCuandoEstaPorExpirarYGuardaElRefreshRotado() {
        SesionTokens tokens = conectar(Instant.now().plusSeconds(30));
        servidor.expect(requestTo(REFRESH)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(RESPUESTA_NUEVA, MediaType.APPLICATION_JSON));

        assertThat(sesion.accessTokenVigente()).isEqualTo("nuevo");
        assertThat(tokens.getRefreshToken()).isEqualTo("r2");
        servidor.verify();
    }

    @Test
    void refreshRechazadoLanzaSesionExpirada() {
        conectar(Instant.now().plusSeconds(30));
        servidor.expect(requestTo(REFRESH)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sesion.accessTokenVigente())
                .isInstanceOf(SesionExpiradaException.class);
    }

    @Test
    void sinSesionLanzaSesionExpirada() {
        assertThatThrownBy(() -> sesion.accessTokenVigente())
                .isInstanceOf(SesionExpiradaException.class);
    }

    @Test
    void unAfterRetryRenuevaUnaVezYRepiteLaLlamada() {
        SesionTokens tokens = conectar(Instant.now().plusSeconds(600));
        servidor.expect(requestTo(REFRESH))
                .andRespond(withSuccess(RESPUESTA_NUEVA, MediaType.APPLICATION_JSON));

        String resultado = sesion.ejecutar(token -> {
            if (token.equals("viejo")) {
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
            }
            return token;
        });

        assertThat(resultado).isEqualTo("nuevo");
        assertThat(tokens.getRefreshToken()).isEqualTo("r2");
        servidor.verify();
    }

    @Test
    void unRefreshRechazadoTrasUn401LanzaSesionExpirada() {
        conectar(Instant.now().plusSeconds(600));
        servidor.expect(requestTo(REFRESH)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sesion.ejecutar(token -> {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        })).isInstanceOf(SesionExpiradaException.class);
    }

    @Test
    void otrosErroresNoRenuevanLaSesion() {
        conectar(Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> sesion.ejecutar(token -> {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        })).isInstanceOf(HttpClientErrorException.class);
        servidor.verify();
    }

    @Test
    void siOtraPeticionYaRenovoAdoptaElTokenDelAlmacenSinUsarElRefreshViejo() {
        SesionTokens tokens = conectar(Instant.now().plusSeconds(30));
        Session guardada = sesionGuardada("ya-renovado", "r2");
        when(sesiones.findById("sid-1")).thenReturn(guardada);

        sesion.renovarProtegido("sid-1", tokens);

        assertThat(tokens.getAccessToken()).isEqualTo("ya-renovado");
        assertThat(tokens.getRefreshToken()).isEqualTo("r2");
        servidor.verify();
        verify(redis).delete("ui:renovacion:sid-1");
    }

    @Test
    void siNadieRenovoHaceUnSoloRefreshYLiberaElLock() {
        SesionTokens tokens = conectar(Instant.now().plusSeconds(30));
        Session guardada = sesionGuardada("viejo", "r1");
        when(sesiones.findById("sid-2")).thenReturn(guardada);
        servidor.expect(requestTo(REFRESH)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(RESPUESTA_NUEVA, MediaType.APPLICATION_JSON));

        sesion.renovarProtegido("sid-2", tokens);

        assertThat(tokens.getAccessToken()).isEqualTo("nuevo");
        servidor.verify();
        verify(redis).delete("ui:renovacion:sid-2");
    }
}
