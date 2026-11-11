package com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Vuelve a cifrar, por lotes, los valores de una columna que no están en la versión activa de la clave.
 * <p>
 * Es reanudable: solo pide filas pendientes, así que si el proceso se corta, al volver a correrlo continúa
 * donde quedó. Cada fila se actualiza solo si su valor sigue siendo el que se leyó, para no pisar una escritura
 * concurrente de la aplicación.
 */
public final class RecifradorPorLotes {

    /** Fila pendiente: su id y el valor cifrado tal como está en la base. */
    public record Fila(Object id, String valor) {
    }

    /** Acceso a la base; la implementación JDBC vive aparte para poder probar la lógica sin base de datos. */
    public interface Almacen {

        /** Hasta {@code limite} filas con valor no nulo que no están en la versión activa. */
        List<Fila> pendientes(String tabla, String columna, int limite);

        /** Cambia el valor solo si la columna aún vale {@code valorAnterior}. Devuelve si se cambió. */
        boolean actualizar(String tabla, String columna, Object id, String valorAnterior, String valorNuevo);
    }

    private final Almacen almacen;
    private final int tamanoLote;
    private final UnaryOperator<String> recifrar;

    /** Recibe el acceso a la base, el tamaño de lote y la función que re-cifra un valor. */
    public RecifradorPorLotes(Almacen almacen, int tamanoLote, UnaryOperator<String> recifrar) {
        if (tamanoLote < 1) {
            throw new IllegalArgumentException("El tamaño de lote debe ser al menos 1");
        }
        this.almacen = almacen;
        this.tamanoLote = tamanoLote;
        this.recifrar = recifrar;
    }

    /** Recorre la columna hasta que no queden filas pendientes. Devuelve cuántas se re-cifraron. */
    public int ejecutar(String tabla, String columna) {
        int recifradas = 0;
        while (true) {
            List<Fila> lote = almacen.pendientes(tabla, columna, tamanoLote);
            if (lote.isEmpty()) {
                return recifradas;
            }
            for (Fila fila : lote) {
                String nuevo = recifrar.apply(fila.valor());
                if (!almacen.actualizar(tabla, columna, fila.id(), fila.valor(), nuevo)) {
                    throw new IllegalStateException(
                            "No se actualizó " + tabla + "." + columna + " para el id " + fila.id()
                                    + ": el valor cambió durante el proceso. Vuelva a ejecutar.");
                }
                recifradas++;
            }
        }
    }
}
