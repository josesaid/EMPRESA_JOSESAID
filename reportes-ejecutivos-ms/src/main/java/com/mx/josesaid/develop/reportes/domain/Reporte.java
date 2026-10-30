package com.mx.josesaid.develop.reportes.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reporte de un periodo. BORRADOR se consolida en vivo; al cerrarse, guarda una foto y no vuelve a cambiar. */
public class Reporte {

    private final UUID id;
    private final Periodo periodo;
    private final Instant creadoUtc;
    private EstatusReporte estatus;
    private Instant cerradoUtc;
    private List<IndicadorConsolidado> indicadoresCerrados;

    private Reporte(UUID id, Periodo periodo, Instant creadoUtc, EstatusReporte estatus, Instant cerradoUtc,
                    List<IndicadorConsolidado> indicadoresCerrados) {
        this.id = id;
        this.periodo = periodo;
        this.creadoUtc = creadoUtc;
        this.estatus = estatus;
        this.cerradoUtc = cerradoUtc;
        this.indicadoresCerrados = List.copyOf(indicadoresCerrados);
    }

    public static Reporte abrir(Periodo periodo, Instant ahora) {
        return new Reporte(UUID.randomUUID(), periodo, ahora, EstatusReporte.BORRADOR, null, List.of());
    }

    public static Reporte reconstruir(UUID id, Periodo periodo, Instant creadoUtc, EstatusReporte estatus,
                                      Instant cerradoUtc, List<IndicadorConsolidado> indicadoresCerrados) {
        return new Reporte(id, periodo, creadoUtc, estatus, cerradoUtc, indicadoresCerrados);
    }

    public void exigirAbierto() {
        if (estatus == EstatusReporte.CERRADO) {
            throw new ConflictoException("El reporte " + periodo + " está cerrado; no admite cambios");
        }
    }

    public void cerrar(List<IndicadorConsolidado> consolidados, Instant ahora) {
        exigirAbierto();
        if (consolidados.isEmpty()) {
            throw new ReglaDeNegocioException("No hay indicadores para cerrar el periodo " + periodo);
        }
        this.estatus = EstatusReporte.CERRADO;
        this.cerradoUtc = ahora;
        this.indicadoresCerrados = List.copyOf(consolidados);
    }

    public UUID getId() { return id; }
    public Periodo getPeriodo() { return periodo; }
    public Instant getCreadoUtc() { return creadoUtc; }
    public EstatusReporte getEstatus() { return estatus; }
    public Instant getCerradoUtc() { return cerradoUtc; }
    public List<IndicadorConsolidado> getIndicadoresCerrados() { return indicadoresCerrados; }
}
