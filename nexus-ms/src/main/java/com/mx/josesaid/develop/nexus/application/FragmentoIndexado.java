package com.mx.josesaid.develop.nexus.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Fragmento de una política listo para guardarse, con su vector.
 *
 * @param politicaId identificador del politica, de tipo {@code String}.
 * @param indice indice, de tipo {@code int}.
 * @param titulo título, de tipo {@code String}.
 * @param vigencia vigencia del registro, de tipo {@code LocalDate}.
 * @param roles roles asignados, de tipo {@code List<String>}.
 * @param texto texto libre, de tipo {@code String}.
 * @param vector vector, de tipo {@code float[]}.
 */
public record FragmentoIndexado(String politicaId, int indice, String titulo, LocalDate vigencia,
                                List<String> roles, String texto, float[] vector) {
}
