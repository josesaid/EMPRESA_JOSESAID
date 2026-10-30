package com.mx.josesaid.develop.documentos.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con PostgreSQL, Kafka y almacenamiento cifrado reales (Testcontainers). Requiere Docker. */
@SpringBootTest(properties = {
        "documentos.jwt.secreto=clave-de-prueba-de-al-menos-32-bytes-xx",
        "documentos.cifrado.clave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "documentos.almacen.directorio=${java.io.tmpdir}/documentos-it-${random.uuid}"})
@AutoConfigureMockMvc
@Testcontainers
class DocumentosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Test
    void versionesDescargaYRetencionFuncionanExtremoAExtremo() throws Exception {
        String datos = "{\"tipo\":\"CONTRATO\",\"titulo\":\"Contrato marco\",\"area\":\"Administración y legal\"}";
        MvcResult creado = mvc.perform(multipart("/v1/documentos")
                        .file(archivo("contrato v1")).file(parteDatos(datos)).with(rol("LEGAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionActual").value(1))
                .andReturn();
        String id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

        mvc.perform(multipart("/v1/documentos/" + id + "/versiones")
                        .file(archivo("contrato v2")).with(rol("LEGAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionActual").value(2));

        // Mismo contenido que la versión actual: no crea versión
        mvc.perform(multipart("/v1/documentos/" + id + "/versiones")
                        .file(archivo("contrato v2")).with(rol("LEGAL")))
                .andExpect(status().isConflict());

        mvc.perform(get("/v1/documentos/" + id + "/versiones/1/descarga").with(rol("LEGAL")))
                .andExpect(status().isOk())
                .andExpect(content().bytes("contrato v1".getBytes(StandardCharsets.UTF_8)));

        // Dentro de la retención no se elimina
        mvc.perform(post("/v1/documentos/" + id + "/eliminacion").with(rol("LEGAL"))
                        .contentType("application/json").content("{\"motivo\":\"Prueba\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void permisosPorVencerYRolesSeAplican() throws Exception {
        String permiso = """
                {"tipo":"PERMISO","titulo":"Permiso ambiental","area":"Operaciones","venceEn":"%s"}
                """.formatted(LocalDate.now().plusDays(5));

        // DIRECCION solo consulta: no puede cargar
        mvc.perform(multipart("/v1/documentos").file(archivo("permiso")).file(parteDatos(permiso))
                        .with(rol("DIRECCION")))
                .andExpect(status().isForbidden());

        mvc.perform(multipart("/v1/documentos").file(archivo("permiso")).file(parteDatos(permiso))
                        .with(rol("LEGAL")))
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/documentos/por-vencer?dias=10").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titulo").value(hasItem("Permiso ambiental")));

        mvc.perform(get("/v1/documentos/por-vencer?dias=-1").with(rol("LEGAL")))
                .andExpect(status().isBadRequest());
    }

    private static MockMultipartFile archivo(String contenido) {
        return new MockMultipartFile("archivo", "documento.pdf", "application/pdf",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile parteDatos(String json) {
        return new MockMultipartFile("datos", "", "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
