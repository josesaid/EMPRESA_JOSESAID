package com.mx.josesaid.develop.nexus.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.AuditoriaConsultas;
import com.mx.josesaid.develop.nexus.application.RegistroConsulta;
import com.mx.josesaid.develop.nexus.application.RespuestaAsistente;

class NexusControladorIdentidadTest {

    private static final String SUBJECT = "4962f8ee-4e35-4cb6-9919-8ad95d37c245";

    @Test
    void auditaElCorreoDelUsuarioCuandoElTokenLoTrae() {
        AsistenteIA asistente = mock(AsistenteIA.class);
        AuditoriaConsultas auditoria = mock(AuditoriaConsultas.class);
        when(asistente.consultar(any())).thenReturn(new RespuestaAsistente("respuesta"));
        NexusControlador controlador = new NexusControlador(asistente, auditoria);

        controlador.consultar(new ConsultaRequest("Dame clientes"), jwt(SUBJECT, "admin@empresa.mx"));

        ArgumentCaptor<RegistroConsulta> captor = ArgumentCaptor.forClass(RegistroConsulta.class);
        verify(auditoria).registrar(captor.capture());
        assertEquals("admin@empresa.mx", captor.getValue().usuario());
    }

    @Test
    void usaElIdentificadorCuandoElTokenNoTraeCorreo() {
        AsistenteIA asistente = mock(AsistenteIA.class);
        AuditoriaConsultas auditoria = mock(AuditoriaConsultas.class);
        when(asistente.consultar(any())).thenReturn(new RespuestaAsistente("respuesta"));
        NexusControlador controlador = new NexusControlador(asistente, auditoria);

        controlador.consultar(new ConsultaRequest("Dame clientes"), jwt(SUBJECT, null));

        ArgumentCaptor<RegistroConsulta> captor = ArgumentCaptor.forClass(RegistroConsulta.class);
        verify(auditoria).registrar(captor.capture());
        assertEquals(SUBJECT, captor.getValue().usuario());
    }

    private static Jwt jwt(String subject, String correo) {
        Instant ahora = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(300));
        if (correo != null) {
            builder.claim("correo", correo);
        }
        return builder.build();
    }
}
