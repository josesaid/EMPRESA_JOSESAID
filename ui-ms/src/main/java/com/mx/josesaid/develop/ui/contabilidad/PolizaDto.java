package com.mx.josesaid.develop.ui.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Forma de una póliza tal como la devuelve contabilidad-ms.
 *
 * @param id componente de tipo {@code UUID}.
 * @param periodo componente de tipo {@code String}.
 * @param tipo componente de tipo {@code String}.
 * @param fecha componente de tipo {@code LocalDate}.
 * @param estatus componente de tipo {@code String}.
 * @param reversaDe componente de tipo {@code UUID}.
 * @param total componente de tipo {@code BigDecimal}.
 * @param movimientos componente de tipo {@code List<MovimientoDto>}.
 */
public record PolizaDto(UUID id, String periodo, String tipo, LocalDate fecha, String estatus,
                        UUID reversaDe, BigDecimal total, List<MovimientoDto> movimientos) {

    /**
     * Línea de una póliza: cuenta afectada y los importes de cargo y abono.
     *
     * @param cuentaId componente de tipo {@code UUID}.
     * @param cargo componente de tipo {@code BigDecimal}.
     * @param abono componente de tipo {@code BigDecimal}.
     */
    public record MovimientoDto(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
