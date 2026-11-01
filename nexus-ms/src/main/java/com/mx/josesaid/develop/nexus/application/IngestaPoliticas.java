package com.mx.josesaid.develop.nexus.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Trocea cada política en líneas con contenido (cada punto o regla es un fragmento),
 * genera sus embeddings y reemplaza lo que hubiera guardado para esa política.
 */
public final class IngestaPoliticas {

    private final Embedder embedder;
    private final AlmacenFragmentos almacen;

    public IngestaPoliticas(Embedder embedder, AlmacenFragmentos almacen) {
        this.embedder = embedder;
        this.almacen = almacen;
    }

    /** Devuelve cuántos fragmentos quedaron guardados. */
    public int ingerir(List<Politica> politicas) {
        int total = 0;
        for (Politica politica : politicas) {
            List<String> trozos = trocear(politica);
            if (trozos.isEmpty()) {
                almacen.reemplazar(politica.id(), List.of());
                continue;
            }
            // El título va en el texto que se embebe para que la búsqueda tenga contexto; se guarda solo el trozo
            List<String> paraEmbeber = trozos.stream().map(t -> politica.titulo() + ". " + t).toList();
            List<float[]> vectores = embedder.embeber(paraEmbeber, Embedder.TipoEntrada.DOCUMENTO);
            if (vectores.size() != trozos.size()) {
                throw new IllegalStateException("El proveedor de embeddings devolvió " + vectores.size()
                        + " vectores para " + trozos.size() + " fragmentos de " + politica.id());
            }
            List<FragmentoIndexado> fragmentos = new ArrayList<>();
            for (int i = 0; i < trozos.size(); i++) {
                fragmentos.add(new FragmentoIndexado(politica.id(), i, politica.titulo(), politica.vigencia(),
                        politica.roles(), trozos.get(i), vectores.get(i)));
            }
            almacen.reemplazar(politica.id(), fragmentos);
            total += fragmentos.size();
        }
        return total;
    }

    static List<String> trocear(Politica politica) {
        List<String> trozos = new ArrayList<>();
        for (String linea : politica.cuerpo().split("\\R")) {
            String texto = linea.trim();
            if (texto.isEmpty() || texto.startsWith("#")) {
                continue;
            }
            trozos.add(texto);
        }
        return trozos;
    }
}
