package com.mx.josesaid.develop.documentos.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Documento legal con versiones. Cada cambio de contenido crea una versión nueva; las publicadas no cambian.
 * Un documento vencido no recibe versiones: se registra uno nuevo. Eliminar exige retención vencida y motivo.
 */
public class Documento {

    private final UUID id;
    private final TipoDocumento tipo;
    private final String titulo;
    private final String area;
    private final LocalDate venceEn;
    private final LocalDate retencionHasta;
    private final List<VersionDocumento> versiones;
    private boolean eliminado;
    private String motivoEliminacion;
    private Instant eliminadoUtc;
    private boolean revisionRetencion;
    private boolean alertaVencimientoEmitida;

    private Documento(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                      LocalDate retencionHasta, List<VersionDocumento> versiones, boolean eliminado,
                      String motivoEliminacion, Instant eliminadoUtc, boolean revisionRetencion,
                      boolean alertaVencimientoEmitida) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.area = area;
        this.venceEn = venceEn;
        this.retencionHasta = retencionHasta;
        this.versiones = new ArrayList<>(versiones);
        this.eliminado = eliminado;
        this.motivoEliminacion = motivoEliminacion;
        this.eliminadoUtc = eliminadoUtc;
        this.revisionRetencion = revisionRetencion;
        this.alertaVencimientoEmitida = alertaVencimientoEmitida;
    }

    /** Crea el documento con su versión 1. La retención se calcula desde hoy según el tipo. */
    public static Documento crear(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                                  LocalDate hoy, String hashSha256, String ref, Instant ahora) {
        if (tipo == null) {
            throw new SolicitudInvalidaException("El tipo de documento es obligatorio");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new SolicitudInvalidaException("El título es obligatorio");
        }
        if (area == null || area.isBlank()) {
            throw new SolicitudInvalidaException("El área es obligatoria");
        }
        if (tipo == TipoDocumento.PERMISO && venceEn == null) {
            throw new ReglaDeNegocioException("Un permiso requiere fecha de vencimiento");
        }
        if (venceEn != null && venceEn.isBefore(hoy)) {
            throw new ReglaDeNegocioException("La fecha de vencimiento ya pasó; registre un documento vigente");
        }
        List<VersionDocumento> primera = List.of(new VersionDocumento(UUID.randomUUID(), 1, hashSha256, ref, ahora));
        return new Documento(id, tipo, titulo.trim(), area.trim(), venceEn, hoy.plusYears(tipo.retencionAnios()),
                primera, false, null, null, false, false);
    }

    public static Documento reconstruir(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                                        LocalDate retencionHasta, List<VersionDocumento> versiones, boolean eliminado,
                                        String motivoEliminacion, Instant eliminadoUtc, boolean revisionRetencion,
                                        boolean alertaVencimientoEmitida) {
        return new Documento(id, tipo, titulo, area, venceEn, retencionHasta, versiones, eliminado,
                motivoEliminacion, eliminadoUtc, revisionRetencion, alertaVencimientoEmitida);
    }

    /** Lanza error si la nueva versión no puede crearse. Se llama antes de guardar el archivo. */
    public void validarNuevaVersion(String hashSha256, LocalDate hoy) {
        if (eliminado) {
            throw new ConflictoException("El documento fue eliminado; no admite versiones");
        }
        if (vencido(hoy)) {
            throw new ConflictoException("El documento venció; registre un documento nuevo");
        }
        if (hashSha256.equals(ultimaVersion().hashSha256())) {
            throw new ConflictoException("El contenido no cambió; no se crea una versión nueva");
        }
    }

    public VersionDocumento agregarVersion(String hashSha256, String ref, LocalDate hoy, Instant ahora) {
        validarNuevaVersion(hashSha256, hoy);
        VersionDocumento nueva = new VersionDocumento(UUID.randomUUID(), getVersionActual() + 1, hashSha256, ref, ahora);
        versiones.add(nueva);
        return nueva;
    }

    public void eliminar(String motivo, LocalDate hoy, Instant ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw new SolicitudInvalidaException("El motivo de eliminación es obligatorio");
        }
        if (eliminado) {
            throw new ConflictoException("El documento ya fue eliminado");
        }
        if (!hoy.isAfter(retencionHasta)) {
            throw new ConflictoException("No se puede eliminar dentro del periodo de retención (hasta " + retencionHasta + ")");
        }
        this.eliminado = true;
        this.motivoEliminacion = motivo.trim();
        this.eliminadoUtc = ahora;
    }

    public boolean vencido(LocalDate hoy) {
        return venceEn != null && venceEn.isBefore(hoy);
    }

    /** Un permiso entra en alerta una sola vez, cuando su vencimiento cae dentro del umbral. */
    public boolean necesitaAlertaVencimiento(LocalDate hoy, int umbralDias) {
        if (tipo != TipoDocumento.PERMISO || eliminado || alertaVencimientoEmitida || venceEn == null) {
            return false;
        }
        return !venceEn.isBefore(hoy) && !venceEn.isAfter(hoy.plusDays(umbralDias));
    }

    public void registrarAlertaVencimiento() {
        this.alertaVencimientoEmitida = true;
    }

    public boolean necesitaRevisionRetencion(LocalDate hoy) {
        return !eliminado && !revisionRetencion && hoy.isAfter(retencionHasta);
    }

    public void marcarRevisionRetencion() {
        this.revisionRetencion = true;
    }

    public Optional<VersionDocumento> version(int numero) {
        return versiones.stream().filter(v -> v.numero() == numero).findFirst();
    }

    private VersionDocumento ultimaVersion() {
        return versiones.get(versiones.size() - 1);
    }

    public UUID getId() { return id; }
    public TipoDocumento getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getArea() { return area; }
    public LocalDate getVenceEn() { return venceEn; }
    public LocalDate getRetencionHasta() { return retencionHasta; }
    public List<VersionDocumento> getVersiones() { return Collections.unmodifiableList(versiones); }
    public int getVersionActual() { return versiones.size(); }
    public boolean isEliminado() { return eliminado; }
    public String getMotivoEliminacion() { return motivoEliminacion; }
    public Instant getEliminadoUtc() { return eliminadoUtc; }
    public boolean isRevisionRetencion() { return revisionRetencion; }
    public boolean isAlertaVencimientoEmitida() { return alertaVencimientoEmitida; }
}
