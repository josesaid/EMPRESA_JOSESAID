package com.mx.josesaid.develop.ui.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Forma de una póliza tal como la devuelve contabilidad-ms.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param periodo periodo de referencia (por ejemplo, año y mes), de tipo {@code String}.
 * @param tipo tipo o clasificación del registro, de tipo {@code String}.
 * @param fecha fecha del registro, de tipo {@code LocalDate}.
 * @param estatus estado actual del registro, de tipo {@code String}.
 * @param reversaDe reversa de, de tipo {@code UUID}.
 * @param total importe total, de tipo {@code BigDecimal}.
 * @param movimientos movimientos, de tipo {@code List<MovimientoDto>}.
 */
public record PolizaDto(UUID id, String periodo, String tipo, LocalDate fecha, String estatus,
                        UUID reversaDe, BigDecimal total, List<MovimientoDto> movimientos) {

    /**
     * Línea de una póliza: cuenta afectada y los importes de cargo y abono.
     *
     * @param cuentaId identificador de la cuenta contable, de tipo {@code UUID}.
     * @param cargo importe del cargo, de tipo {@code BigDecimal}.
     * @param abono importe del abono, de tipo {@code BigDecimal}.
     */
    public record MovimientoDto(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
