package com.mx.josesaid.develop.nexus.comun.prompts;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Carga prompts versionados desde resources.
 * Cada prompt vive en nexus/prompts/{nombre}/v{N}.txt. El archivo nexus/prompts/indice.properties
 * indica la versión vigente de cada uno. Las versiones anteriores se conservan para revertir.
 */
public final class RepositorioPrompts {

    static final String RAIZ = "nexus/prompts/";
    static final String INDICE = RAIZ + "indice.properties";

    private final ClassLoader cargador;
    private final Map<String, Integer> vigentes;

    public RepositorioPrompts() {
        this(RepositorioPrompts.class.getClassLoader());
    }

    public RepositorioPrompts(ClassLoader cargador) {
        this.cargador = cargador;
        this.vigentes = cargarIndice();
    }

    /** Versión vigente del prompt. */
    public PromptVersionado obtener(String nombre) {
        Integer version = vigentes.get(nombre);
        if (version == null) {
            throw new IllegalArgumentException("Prompt sin versión vigente: " + nombre);
        }
        return obtener(nombre, version);
    }

    /** Una versión concreta, por ejemplo para revertir. */
    public PromptVersionado obtener(String nombre, int version) {
        return leer(nombre, version)
                .map(texto -> new PromptVersionado(nombre, version, texto))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la versión " + version + " del prompt " + nombre));
    }

    public Map<String, Integer> versionesVigentes() {
        return Map.copyOf(vigentes);
    }

    private Optional<String> leer(String nombre, int version) {
        String ruta = RAIZ + nombre + "/v" + version + ".txt";
        try (InputStream entrada = cargador.getResourceAsStream(ruta)) {
            if (entrada == null) {
                return Optional.empty();
            }
            return Optional.of(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }

    private Map<String, Integer> cargarIndice() {
        Properties props = new Properties();
        try (InputStream entrada = cargador.getResourceAsStream(INDICE)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta " + INDICE);
            }
            props.load(new java.io.InputStreamReader(entrada, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + INDICE, e);
        }
        Map<String, Integer> mapa = new java.util.HashMap<>();
        for (String clave : props.stringPropertyNames()) {
            mapa.put(clave, Integer.parseInt(props.getProperty(clave).trim()));
        }
        return mapa;
    }
}
