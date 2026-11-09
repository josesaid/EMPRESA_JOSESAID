package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

class HerramientasDocumentosEliminarTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000004";
    private static final String URL_DOCUMENTO = "http://gateway/documentos-cumplimiento-ms/v1/documentos/" + ID;

    private final Map<UUID, AccionPendiente> almacen = new HashMap<>();
    private AccionesPendientes acciones;
    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        AlmacenAcciones repo = new AlmacenAcciones() {
            @Override
            public void guardar(AccionPendiente a) {
                almacen.put(a.id(), a);
            }

            @Override
            public Optional<AccionPendiente> buscar(UUID id) {
                return Optional.ofNullable(almacen.get(id));
            }
        };
        EjecutorAcciones ejecutor = (tipo, payload) -> {
            throw new AssertionError("preparar no debe ejecutar la acción");
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC);
        acciones = new AccionesPendientes(repo, ejecutor, reloj);
        builder = RestClient.builder().baseUrl("http://gateway");
        servidor = MockRestServiceServer.bindTo(builder).build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-legal")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_LEGAL"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasDocumentos herramientas() {
        return new HerramientasDocumentos(builder.build(), acciones);
    }

    @Test
    void eliminacionSePreparaSinEjecutarla() {
        servidor.expect(requestTo(URL_DOCUMENTO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().solicitarEliminacionDocumento(ID, "  Contrato vencido  ");

        assertThat(texto).contains("no ejecutada").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_DOCUMENTO_ELIMINAR);
            assertThat(a.payload()).contains("\"motivo\":\"Contrato vencido\"");
        });
        servidor.verify();
    }

    @Test
    void documentoInexistenteNoPreparaAccion() {
        servidor.expect(requestTo(URL_DOCUMENTO)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        String texto = herramientas().solicitarEliminacionDocumento(ID, "Duplicado");

        assertThat(texto).isEqualTo("No encontré ese documento.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void sinMotivoNoPreparaAccion() {
        String texto = herramientas().solicitarEliminacionDocumento(ID, "   ");

        assertThat(texto).isEqualTo("El motivo de la eliminación es obligatorio.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void motivoLargoNoPreparaAccion() {
        String texto = herramientas().solicitarEliminacionDocumento(ID, "x".repeat(301));

        assertThat(texto).contains("300 caracteres");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void idInvalidoNoPreparaAccion() {
        String texto = herramientas().solicitarEliminacionDocumento("no-es-uuid", "Duplicado");

        assertThat(texto).isEqualTo("El identificador del documento no es válido.");
        assertThat(almacen).isEmpty();
    }
}
