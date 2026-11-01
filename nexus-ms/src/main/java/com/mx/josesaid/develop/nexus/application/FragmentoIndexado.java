package com.mx.josesaid.develop.nexus.application;

import java.time.LocalDate;
import java.util.List;

/** Fragmento de una política listo para guardarse, con su vector. */
public record FragmentoIndexado(String politicaId, int indice, String titulo, LocalDate vigencia,
                                List<String> roles, String texto, float[] vector) {
}
