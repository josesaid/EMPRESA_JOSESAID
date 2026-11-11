package com.mx.josesaid.develop.nexus.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Fragmento de una política listo para guardarse, con su vector.
 *
 * @param politicaId componente de tipo {@code String}.
 * @param indice componente de tipo {@code int}.
 * @param titulo componente de tipo {@code String}.
 * @param vigencia componente de tipo {@code LocalDate}.
 * @param roles componente de tipo {@code List<String>}.
 * @param texto componente de tipo {@code String}.
 * @param vector componente de tipo {@code float[]}.
 */
public record FragmentoIndexado(String politicaId, int indice, String titulo, LocalDate vigencia,
                                List<String> roles, String texto, float[] vector) {
}
