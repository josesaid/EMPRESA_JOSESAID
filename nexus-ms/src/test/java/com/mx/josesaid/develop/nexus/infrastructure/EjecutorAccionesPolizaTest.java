package com.mx.josesaid.develop.nexus.infrastructure;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

/** La póliza solo llega a contabilidad cuando se ejecuta una acción ya confirmada. */
class EjecutorAccionesPolizaTest {

    @Test
    void polizaConfirmadaSeEnviaAContabilidad() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gateway");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        String payload = "{\"tipo\":\"INGRESO\",\"fecha\":\"2026-11-15\",\"movimientos\":["
                + "{\"cuentaId\":\"00000000-0000-4000-8000-000000000101\",\"cargo\":1000,\"abono\":0},"
                + "{\"cuentaId\":\"00000000-0000-4000-8000-000000000401\",\"cargo\":0,\"abono\":1000}]}";
        servidor.expect(requestTo("http://gateway/contabilidad-ms/v1/polizas"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(payload))
                .andRespond(withSuccess("{\"id\":\"p1\"}", MediaType.APPLICATION_JSON));

        new EjecutorAccionesGateway(builder.build()).ejecutar(AccionesPendientes.TIPO_POLIZA_REGISTRAR, payload);

        servidor.verify();
    }
}
