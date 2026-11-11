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

    /** Constructor privado: los reportes se crean con abrir o reconstruir. */
    private Reporte(UUID id, Periodo periodo, Instant creadoUtc, EstatusReporte estatus, Instant cerradoUtc,
                    List<IndicadorConsolidado> indicadoresCerrados) {
        this.id = id;
        this.periodo = periodo;
        this.creadoUtc = creadoUtc;
        this.estatus = estatus;
        this.cerradoUtc = cerradoUtc;
        this.indicadoresCerrados = List.copyOf(indicadoresCerrados);
    }

    /** Abre un reporte nuevo en BORRADOR para un periodo. */
    public static Reporte abrir(Periodo periodo, Instant ahora) {
        return new Reporte(UUID.randomUUID(), periodo, ahora, EstatusReporte.BORRADOR, null, List.of());
    }

    /** Reconstruye un reporte desde la base de datos sin aplicar reglas. */
    public static Reporte reconstruir(UUID id, Periodo periodo, Instant creadoUtc, EstatusReporte estatus,
                                      Instant cerradoUtc, List<IndicadorConsolidado> indicadoresCerrados) {
        return new Reporte(id, periodo, creadoUtc, estatus, cerradoUtc, indicadoresCerrados);
    }

    /** Lanza ConflictoException si el reporte ya está cerrado. Toda modificación pasa por aquí. */
    public void exigirAbierto() {
        if (estatus == EstatusReporte.CERRADO) {
            throw new ConflictoException("El reporte " + periodo + " está cerrado; no admite cambios");
        }
    }

    /** Cierra el reporte con sus indicadores consolidados. Un reporte sin indicadores no se puede cerrar. */
    public void cerrar(List<IndicadorConsolidado> consolidados, Instant ahora) {
        exigirAbierto();
        if (consolidados.isEmpty()) {
            throw new ReglaDeNegocioException("No hay indicadores para cerrar el periodo " + periodo);
        }
        this.estatus = EstatusReporte.CERRADO;
        this.cerradoUtc = ahora;
        this.indicadoresCerrados = List.copyOf(consolidados);
    }

    /** Devuelve el identificador del reporte. */
    public UUID getId() { return id; }
    /** Devuelve el periodo del reporte. */
    public Periodo getPeriodo() { return periodo; }
    /** Devuelve la fecha de creación en UTC. */
    public Instant getCreadoUtc() { return creadoUtc; }
    /** Devuelve el estatus del reporte. */
    public EstatusReporte getEstatus() { return estatus; }
    /** Devuelve la fecha de cierre en UTC, o nula si sigue abierto. */
    public Instant getCerradoUtc() { return cerradoUtc; }
    /** Devuelve los indicadores consolidados al cerrar el reporte. */
    public List<IndicadorConsolidado> getIndicadoresCerrados() { return indicadoresCerrados; }
}
