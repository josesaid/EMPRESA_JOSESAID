package com.mx.josesaid.develop.nexus.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.mx.josesaid.develop.nexus.application.Politica;

/** Lee las políticas Markdown de classpath:politicas/. El id de cada política es su nombre de archivo sin extensión. */
public final class PoliticasClasspath {

    private static final String PATRON = "classpath*:politicas/*.md";

    private PoliticasClasspath() {
    }

    public static List<Politica> cargar() throws IOException {
        Resource[] recursos = new PathMatchingResourcePatternResolver().getResources(PATRON);
        List<Politica> politicas = new ArrayList<>();
        for (Resource recurso : recursos) {
            String nombre = recurso.getFilename();
            String id = nombre.substring(0, nombre.length() - ".md".length());
            String contenido = new String(recurso.getContentAsByteArray(), StandardCharsets.UTF_8);
            politicas.add(Politica.desde(id, contenido));
        }
        return politicas;
    }
}
