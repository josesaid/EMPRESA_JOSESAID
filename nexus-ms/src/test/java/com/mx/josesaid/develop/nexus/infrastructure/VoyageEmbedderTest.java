package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.Embedder;

class VoyageEmbedderTest {

    @Test
    void enviaClaveModeloYTipoDeEntrada() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://voyage.test");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://voyage.test/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer clave-de-prueba"))
                .andExpect(content().json("{\"input\":[\"uno\"],\"model\":\"voyage-3\",\"input_type\":\"query\"}"))
                .andRespond(withSuccess("{\"data\":[{\"index\":0,\"embedding\":[0.5,0.25]}]}",
                        MediaType.APPLICATION_JSON));

        List<float[]> vectores = new VoyageEmbedder(builder.build(), "clave-de-prueba", "voyage-3")
                .embeber(List.of("uno"), Embedder.TipoEntrada.CONSULTA);

        assertThat(vectores).hasSize(1);
        assertThat(vectores.get(0)).containsExactly(0.5f, 0.25f);
        servidor.verify();
    }

    @Test
    void ordenaLosVectoresPorIndice() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://voyage.test");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://voyage.test/v1/embeddings"))
                .andRespond(withSuccess("{\"data\":[{\"index\":1,\"embedding\":[2]},{\"index\":0,\"embedding\":[1]}]}",
                        MediaType.APPLICATION_JSON));

        List<float[]> vectores = new VoyageEmbedder(builder.build(), "clave", "voyage-3")
                .embeber(List.of("a", "b"), Embedder.TipoEntrada.DOCUMENTO);

        assertThat(vectores.get(0)).containsExactly(1f);
        assertThat(vectores.get(1)).containsExactly(2f);
    }

    @Test
    void reintentaTrasUn429() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://voyage.test");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://voyage.test/v1/embeddings"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo("https://voyage.test/v1/embeddings"))
                .andRespond(withSuccess("{\"data\":[{\"index\":0,\"embedding\":[3]}]}", MediaType.APPLICATION_JSON));

        List<float[]> vectores = new VoyageEmbedder(builder.build(), "clave", "voyage-3", Duration.ZERO)
                .embeber(List.of("uno"), Embedder.TipoEntrada.CONSULTA);

        assertThat(vectores.get(0)).containsExactly(3f);
        servidor.verify();
    }

    @Test
    void noReintentaIndefinidamente() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://voyage.test");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        for (int i = 0; i < VoyageEmbedder.MAX_INTENTOS; i++) {
            servidor.expect(requestTo("https://voyage.test/v1/embeddings"))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        }

        assertThatThrownBy(() -> new VoyageEmbedder(builder.build(), "clave", "voyage-3", Duration.ZERO)
                .embeber(List.of("uno"), Embedder.TipoEntrada.CONSULTA))
                .isInstanceOf(HttpClientErrorException.class);
        servidor.verify();
    }

    @Test
    void sinClaveNoLlamaAlProveedor() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://voyage.test");
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

        assertThatThrownBy(() -> new VoyageEmbedder(builder.build(), "", "voyage-3")
                .embeber(List.of("uno"), Embedder.TipoEntrada.DOCUMENTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VOYAGE_API_KEY");
        servidor.verify();
    }
}
