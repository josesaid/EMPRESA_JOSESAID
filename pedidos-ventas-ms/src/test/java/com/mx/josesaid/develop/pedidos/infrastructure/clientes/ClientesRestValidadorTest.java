package com.mx.josesaid.develop.pedidos.infrastructure.clientes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mx.josesaid.develop.pedidos.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.pedidos.domain.ServicioNoDisponibleException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class ClientesRestValidadorTest {

    private static final UUID CLIENTE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String URL = "http://clientes";

    private MockRestServiceServer servidor;
    private ClientesRestValidador validador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        validador = new ClientesRestValidador(builder, URL, new ObjectMapper());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("tok").header("alg", "RS256").claim("sub", "usuario").build()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clienteActivoPasaYReenviaElToken() {
        servidor.expect(requestTo(URL + "/v1/clientes/" + CLIENTE))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withSuccess("{\"id\":\"" + CLIENTE + "\",\"estatus\":\"ACTIVO\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> validador.exigirActivo(CLIENTE));
        servidor.verify();
    }

    @Test
    void clienteSuspendidoRechaza() {
        servidor.expect(requestTo(URL + "/v1/clientes/" + CLIENTE))
                .andRespond(withSuccess("{\"id\":\"" + CLIENTE + "\",\"estatus\":\"SUSPENDIDO\"}", MediaType.APPLICATION_JSON));

        assertThrows(ReglaDeNegocioException.class, () -> validador.exigirActivo(CLIENTE));
    }

    @Test
    void clienteInexistenteRechaza() {
        servidor.expect(requestTo(URL + "/v1/clientes/" + CLIENTE)).andRespond(withResourceNotFound());

        assertThrows(ReglaDeNegocioException.class, () -> validador.exigirActivo(CLIENTE));
    }

    @Test
    void clientesCaidoDaServicioNoDisponible() {
        servidor.expect(requestTo(URL + "/v1/clientes/" + CLIENTE)).andRespond(withServerError());

        assertThrows(ServicioNoDisponibleException.class, () -> validador.exigirActivo(CLIENTE));
    }
}
