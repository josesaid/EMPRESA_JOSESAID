package com.mx.josesaid.develop.nexus.application;

import java.util.List;

/** Convierte textos en vectores. La implementación concreta (Voyage AI) vive en infrastructure. */
public interface Embedder {

    /** DOCUMENTO para fragmentos que se guardan; CONSULTA para la pregunta del usuario. */
    enum TipoEntrada { DOCUMENTO, CONSULTA }

    List<float[]> embeber(List<String> textos, TipoEntrada tipo);
}
