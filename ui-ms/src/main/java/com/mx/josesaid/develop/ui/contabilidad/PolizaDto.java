package com.mx.josesaid.develop.ui.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Forma de una póliza tal como la devuelve contabilidad-ms. */
public record PolizaDto(UUID id, String periodo, String tipo, LocalDate fecha, String estatus,
                        UUID reversaDe, BigDecimal total, List<MovimientoDto> movimientos) {

    /** Línea de una póliza: cuenta afectada y los importes de cargo y abono. */
    public record MovimientoDto(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
