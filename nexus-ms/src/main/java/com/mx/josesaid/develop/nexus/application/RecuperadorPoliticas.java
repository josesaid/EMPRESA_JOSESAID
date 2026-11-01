package com.mx.josesaid.develop.nexus.application;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Busca en las políticas solo lo que los roles del usuario pueden ver.
 * Sin roles no se consulta nada: el acceso se niega por defecto.
 */
public final class RecuperadorPoliticas {

    private static final int LIMITE_MAXIMO = 10;

    private final Embedder embedder;
    private final AlmacenFragmentos almacen;
    private final Clock reloj;

    public RecuperadorPoliticas(Embedder embedder, AlmacenFragmentos almacen, Clock reloj) {
        this.embedder = embedder;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    public List<ResultadoPolitica> buscar(String pregunta, Set<String> roles, int limite) {
        Set<String> normalizados = roles.stream()
                .filter(Objects::nonNull)
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .filter(r -> !r.isEmpty())
                .collect(toUnmodifiableSet());
        if (normalizados.isEmpty()) {
            return List.of();
        }
        float[] consulta = embedder.embeber(List.of(pregunta), Embedder.TipoEntrada.CONSULTA).get(0);
        return almacen.buscar(normalizados, consulta, LocalDate.now(reloj), Math.min(Math.max(limite, 1), LIMITE_MAXIMO));
    }
}
