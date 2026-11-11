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
 * Purgar borra el contenido físico de un documento eliminado; los metadatos y los hashes se conservan.
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
    private boolean purgado;
    private Instant purgadoUtc;

    /** Constructor privado: los documentos se crean con sus fábricas y no con new. */
    private Documento(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                      LocalDate retencionHasta, List<VersionDocumento> versiones, boolean eliminado,
                      String motivoEliminacion, Instant eliminadoUtc, boolean revisionRetencion,
                      boolean alertaVencimientoEmitida, boolean purgado, Instant purgadoUtc) {
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
        this.purgado = purgado;
        this.purgadoUtc = purgadoUtc;
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
                primera, false, null, null, false, false, false, null);
    }

    /** Reconstruye un documento desde la base de datos sin aplicar reglas de negocio. */
    public static Documento reconstruir(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                                        LocalDate retencionHasta, List<VersionDocumento> versiones, boolean eliminado,
                                        String motivoEliminacion, Instant eliminadoUtc, boolean revisionRetencion,
                                        boolean alertaVencimientoEmitida, boolean purgado, Instant purgadoUtc) {
        return new Documento(id, tipo, titulo, area, venceEn, retencionHasta, versiones, eliminado,
                motivoEliminacion, eliminadoUtc, revisionRetencion, alertaVencimientoEmitida, purgado, purgadoUtc);
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

    /** Agrega una versión nueva con su huella y su referencia de almacenamiento. Devuelve la versión creada. */
    public VersionDocumento agregarVersion(String hashSha256, String ref, LocalDate hoy, Instant ahora) {
        validarNuevaVersion(hashSha256, hoy);
        VersionDocumento nueva = new VersionDocumento(UUID.randomUUID(), getVersionActual() + 1, hashSha256, ref, ahora);
        versiones.add(nueva);
        return nueva;
    }

    /** Elimina el documento de forma lógica. El motivo es obligatorio, no puede eliminarse dos veces y no se permite dentro de la retención. */
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

    /**
     * Purga física: marca el documento como purgado. El llamador borra después los archivos de cada versión.
     * Exige eliminación previa, retención vencida y confirmación tecleada con el identificador del documento.
     */
    public void purgar(String confirmacion, LocalDate hoy, Instant ahora) {
        if (!eliminado) {
            throw new ConflictoException("Solo se purga un documento eliminado");
        }
        if (purgado) {
            throw new ConflictoException("El documento ya fue purgado");
        }
        if (!hoy.isAfter(retencionHasta)) {
            throw new ConflictoException("No se puede purgar dentro del periodo de retención (hasta " + retencionHasta + ")");
        }
        if (confirmacion == null || !confirmacion.trim().equals(id.toString())) {
            throw new SolicitudInvalidaException("La confirmación debe ser el identificador del documento");
        }
        this.purgado = true;
        this.purgadoUtc = ahora;
    }

    /** Indica si el documento ya venció a la fecha indicada. */
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

    /** Marca que la alerta de vencimiento ya se emitió, para no repetirla. */
    public void registrarAlertaVencimiento() {
        this.alertaVencimientoEmitida = true;
    }

    /** Indica si el documento ya cumplió su retención y debe revisarse. */
    public boolean necesitaRevisionRetencion(LocalDate hoy) {
        return !eliminado && !revisionRetencion && hoy.isAfter(retencionHasta);
    }

    /** Marca que la revisión de retención ya se hizo. */
    public void marcarRevisionRetencion() {
        this.revisionRetencion = true;
    }

    /** Devuelve la versión con ese número, si existe. */
    public Optional<VersionDocumento> version(int numero) {
        return versiones.stream().filter(v -> v.numero() == numero).findFirst();
    }

    /** Devuelve la versión más reciente. */
    private VersionDocumento ultimaVersion() {
        return versiones.get(versiones.size() - 1);
    }

    /** Devuelve el identificador del documento. */
    public UUID getId() { return id; }
    /** Devuelve el tipo de documento. */
    public TipoDocumento getTipo() { return tipo; }
    /** Devuelve el título. */
    public String getTitulo() { return titulo; }
    /** Devuelve el área responsable. */
    public String getArea() { return area; }
    /** Devuelve la fecha de vencimiento, si aplica. */
    public LocalDate getVenceEn() { return venceEn; }
    /** Devuelve la fecha hasta la que se conserva el documento. */
    public LocalDate getRetencionHasta() { return retencionHasta; }
    /** Devuelve las versiones en orden, en una vista de solo lectura. */
    public List<VersionDocumento> getVersiones() { return Collections.unmodifiableList(versiones); }
    /** Devuelve el número de la última versión. */
    public int getVersionActual() { return versiones.size(); }
    /** Indica si el documento fue eliminado de forma lógica. */
    public boolean isEliminado() { return eliminado; }
    /** Devuelve el motivo de la eliminación, o nulo si no fue eliminado. */
    public String getMotivoEliminacion() { return motivoEliminacion; }
    /** Devuelve la fecha de eliminación en UTC. */
    public Instant getEliminadoUtc() { return eliminadoUtc; }
    /** Indica si el documento está marcado para revisión de retención. */
    public boolean isRevisionRetencion() { return revisionRetencion; }
    /** Indica si ya se emitió la alerta de vencimiento. */
    public boolean isAlertaVencimientoEmitida() { return alertaVencimientoEmitida; }
    /** Indica si el contenido ya fue purgado. */
    public boolean isPurgado() { return purgado; }
    /** Devuelve la fecha de la purga en UTC. */
    public Instant getPurgadoUtc() { return purgadoUtc; }
}
