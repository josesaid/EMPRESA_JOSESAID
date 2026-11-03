package com.mx.josesaid.develop.ui.contabilidad;

import java.util.UUID;

/** Forma de una cuenta contable tal como la devuelve contabilidad-ms. */
public record CuentaDto(UUID id, String codigo, String nombre, String naturaleza, boolean activa) {
}
