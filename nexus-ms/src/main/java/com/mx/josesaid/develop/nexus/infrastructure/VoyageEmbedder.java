package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.Embedder;

/** Genera embeddings con la API de Voyage AI. La clave viene de VOYAGE_API_KEY, nunca del código. */
public final class VoyageEmbedder implements Embedder {

    static final String RUTA = "/v1/embeddings";

    private final RestClient cliente;
    private final String apiKey;
    private final String modelo;

    public VoyageEmbedder(RestClient cliente, String apiKey, String modelo) {
        this.cliente = cliente;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    @Override
    public List<float[]> embeber(List<String> textos, TipoEntrada tipo) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta VOYAGE_API_KEY para generar embeddings");
        }
        if (textos.isEmpty()) {
            return List.of();
        }
        RespuestaEmbeddings respuesta = cliente.post()
                .uri(RUTA)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "input", textos,
                        "model", modelo,
                        "input_type", tipo == TipoEntrada.CONSULTA ? "query" : "document"))
                .retrieve()
                .body(RespuestaEmbeddings.class);
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
