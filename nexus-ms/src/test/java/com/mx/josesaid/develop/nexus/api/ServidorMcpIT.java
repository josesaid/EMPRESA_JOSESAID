package com.mx.josesaid.develop.nexus.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;

/**
 * Prueba el endpoint /mcp de punta a punta: JWT, lista de herramientas, confirmación con código
 * y permisos por rol. Requiere Docker. Se ejecuta con: mvn verify (failsafe).
 */
@SpringBootTest(properties = "nexus.rag.ingesta-al-arrancar=false")
@AutoConfigureMockMvc
@Testcontainers
class ServidorMcpIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    private static final String VERSION = "2025-06-18";
    private static final String CONTADOR = "u-contador";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AccionesPendientes acciones;

    private int siguienteId = 1;

    private String rpc(String metodo, String paramsJson, String rol, String sesion) throws Exception {
        return rpcBody(metodo, paramsJson, rol, sesion, CONTADOR);
    }

    private String rpcBody(String metodo, String paramsJson, String rol, String sesion, String sujeto) throws Exception {
        String cuerpo = "{\"jsonrpc\":\"2.0\",\"id\":" + siguienteId++ + ",\"method\":\"" + metodo + "\""
                + (paramsJson == null ? "" : ",\"params\":" + paramsJson) + "}";
        var peticion = post("/mcp").contentType("application/json")
                .accept("application/json", "text/event-stream")
                .header("MCP-Protocol-Version", VERSION)
                .content(cuerpo)
                .with(jwt().jwt(j -> j.subject(sujeto)).authorities(new SimpleGrantedAuthority(rol)));
        if (sesion != null) {
            peticion.header("Mcp-Session-Id", sesion);
        }
        MvcResult resultado = mvc.perform(peticion).andReturn();
        return new String(resultado.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Extrae el JSON-RPC de la respuesta, sea JSON directo o un evento SSE. */
    private static JsonNode json(String cuerpo) throws Exception {
        String texto = cuerpo.contains("data:")
                ? cuerpo.substring(cuerpo.indexOf("data:") + 5).split("\n")[0].trim()
                : cuerpo.trim();
        return JSON.readTree(texto);
    }

    private String iniciarSesion(String rol) throws Exception {
        var peticion = post("/mcp").contentType("application/json")
                .accept("application/json", "text/event-stream")
                .content("{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\""
                        + VERSION + "\",\"capabilities\":{},\"clientInfo\":{\"name\":\"prueba\",\"version\":\"1\"}}}")
                .with(jwt().jwt(j -> j.subject(CONTADOR)).authorities(new SimpleGrantedAuthority(rol)));
        MvcResult resultado = mvc.perform(peticion).andExpect(status().isOk()).andReturn();
        String sesion = resultado.getResponse().getHeader("Mcp-Session-Id");
        assertThat(sesion).isNotBlank();
        rpc("notifications/initialized", null, rol, sesion);
        return sesion;
    }

    private String textoDeHerramienta(String cuerpo) throws Exception {
        JsonNode resultado = json(cuerpo).get("result");
        return resultado.get("content").get(0).get("text").asText();
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(post("/mcp").contentType("application/json")
                        .accept("application/json", "text/event-stream")
                        .content("{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"initialize\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listaSoloLasHerramientasDelServidorMcp() throws Exception {
        String sesion = iniciarSesion("ROLE_CONTADOR");

        JsonNode lista = json(rpc("tools/list", "{}", "ROLE_CONTADOR", sesion)).get("result").get("tools");
        java.util.List<String> nombres = new java.util.ArrayList<>();
        lista.forEach(t -> nombres.add(t.get("name").asText()));

        assertThat(nombres).contains("consultar_politicas", "sugerir_poliza", "confirmar_accion", "cancelar_accion");
        assertThat(nombres).doesNotContain("cancelar_pedido", "confirmar_pedido");
    }

    @Test
    void confirmarConCodigoIncorrectoCancelaLaAccion() throws Exception {
        AccionPendiente preparada = acciones.preparar(CONTADOR, AccionesPendientes.TIPO_POLIZA_REGISTRAR,
                "{}", "Póliza de prueba");
        String sesion = iniciarSesion("ROLE_CONTADOR");

        String respuesta = rpc("tools/call",
                "{\"name\":\"confirmar_accion\",\"arguments\":{\"accionId\":\"" + preparada.id()
                        + "\",\"codigo\":\"00000000\"}}", "ROLE_CONTADOR", sesion);

        assertThat(textoDeHerramienta(respuesta)).contains("Código incorrecto");
        assertThat(acciones.consultar(preparada.id(), CONTADOR).estatus())
                .isEqualTo(AccionPendiente.CANCELADA);
    }

    @Test
    void rolSinPermisoNoEjecutaSugerirPoliza() throws Exception {
        String sesion = iniciarSesion("ROLE_COMPRAS");

        String respuesta = rpc("tools/call",
                "{\"name\":\"sugerir_poliza\",\"arguments\":{\"descripcion\":\"Venta de 1000\"}}",
                "ROLE_COMPRAS", sesion);

        assertThat(textoDeHerramienta(respuesta)).contains("Sin permiso");
    }

    @Test
    void confirmarAccionAjenaNoSeEjecuta() throws Exception {
        AccionPendiente ajena = acciones.preparar("u-otro", AccionesPendientes.TIPO_POLIZA_REGISTRAR,
                "{}", "Póliza ajena");
        String sesion = iniciarSesion("ROLE_CONTADOR");

        String respuesta = rpc("tools/call",
                "{\"name\":\"confirmar_accion\",\"arguments\":{\"accionId\":\"" + ajena.id()
                        + "\",\"codigo\":\"00000000\"}}", "ROLE_CONTADOR", sesion);

        assertThat(textoDeHerramienta(respuesta)).contains("no encontrada");
        assertThat(acciones.consultar(ajena.id(), "u-otro").estatus()).isEqualTo(AccionPendiente.PENDIENTE);
    }
}
