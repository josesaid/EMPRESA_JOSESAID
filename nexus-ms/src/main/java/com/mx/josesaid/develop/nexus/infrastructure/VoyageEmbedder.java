package com.mx.josesaid.develop.nexus.infrastructure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.Embedder;

/**
 * Genera embeddings con la API de Voyage AI. La clave viene de VOYAGE_API_KEY, nunca del código.
 * Ante un 429 (límite de solicitudes por minuto) espera y reintenta.
 */
public final class VoyageEmbedder implements Embedder {

    static final String RUTA = "/v1/embeddings";
    static final int MAX_INTENTOS = 4;
    /** Con 3 solicitudes por minuto (cuenta sin método de pago), cada solicitud espera ~20 segundos. */
    static final Duration ESPERA_429 = Duration.ofSeconds(21);

    private final RestClient cliente;
    private final String apiKey;
    private final String modelo;
    private final Duration espera;

    public VoyageEmbedder(RestClient cliente, String apiKey, String modelo) {
        this(cliente, apiKey, modelo, ESPERA_429);
    }

    VoyageEmbedder(RestClient cliente, String apiKey, String modelo, Duration espera) {
        this.cliente = cliente;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.espera = espera;
    }

    @Override
    public List<float[]> embeber(List<String> textos, TipoEntrada tipo) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta VOYAGE_API_KEY para generar embeddings");
        }
        if (textos.isEmpty()) {
            return List.of();
        }
        RespuestaEmbeddings respuesta = pedir(Map.of(
                "input", textos,
                "model", modelo,
                "input_type", tipo == TipoEntrada.CONSULTA ? "query" : "document"));
        if (respuesta == null || respuesta.data() == null || respuesta.data().size() != textos.size()) {
            throw new IllegalStateException("Respuesta de Voyage AI incompleta");
        }
        float[][] vectores = new float[textos.size()][];
        for (Dato dato : respuesta.data()) {
            if (dato.index() < 0 || dato.index() >= vectores.length || dato.embedding() == null) {
                throw new IllegalStateException("Respuesta de Voyage AI con índice inválido");
            }
            vectores[dato.index()] = aFloat(dato.embedding());
        }
        return new ArrayList<>(Arrays.asList(vectores));
    }

    private RespuestaEmbeddings pedir(Map<String, Object> cuerpo) {
        for (int intento = 1; ; intento++) {
            try {
                return cliente.post()
                        .uri(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cuerpo)
                        .retrieve()
                        .body(RespuestaEmbeddings.class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() != 429 || intento >= MAX_INTENTOS) {
                    throw e;
                }
                esperar();
            }
        }
    }

    private void esperar() {
        try {
            Thread.sleep(espera.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido mientras esperaba el límite de Voyage AI", e);
        }
    }

    private static float[] aFloat(List<Float> valores) {
        float[] resultado = new float[valores.size()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = valores.get(i);
        }
        return resultado;
    }

    record RespuestaEmbeddings(List<Dato> data) {
    }

    record Dato(int index, List<Float> embedding) {
    }
}
