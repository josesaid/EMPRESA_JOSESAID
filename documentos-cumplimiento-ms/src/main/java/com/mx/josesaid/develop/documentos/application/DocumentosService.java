package com.mx.josesaid.develop.documentos.application;

import com.mx.josesaid.develop.documentos.domain.ConflictoException;
import com.mx.josesaid.develop.documentos.domain.Documento;
import com.mx.josesaid.develop.documentos.domain.DocumentoRepositorio;
import com.mx.josesaid.develop.documentos.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.documentos.domain.SolicitudInvalidaException;
import com.mx.josesaid.develop.documentos.domain.TipoDocumento;
import com.mx.josesaid.develop.documentos.domain.VersionDocumento;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso documentales: carga, versiones, descarga auditada, eliminación y purga con retención, y alertas. */
@Service
public class DocumentosService {

    private static final Logger log = LoggerFactory.getLogger(DocumentosService.class);
    private static final int MAX_DIAS = 3650;

    private final DocumentoRepositorio documentos;
    private final AlmacenDocumentos almacen;
    private final PublicadorEventos eventos;
    private final Clock reloj;
    private final MeterRegistry metricas;
    private final int umbralDias;

    /** Recibe el repositorio, el almacén de archivos, el publicador de eventos, las métricas y el reloj. */
    public DocumentosService(DocumentoRepositorio documentos, AlmacenDocumentos almacen, PublicadorEventos eventos,
                             Clock reloj, MeterRegistry metricas,
                             @Value("${documentos.alertas.umbral-dias:30}") int umbralDias) {
        this.documentos = documentos;
        this.almacen = almacen;
        this.eventos = eventos;
        this.reloj = reloj;
        this.metricas = metricas;
        this.umbralDias = umbralDias;
    }

    /**
     * Contenido descifrado de una versión, ya verificado contra su hash.
     *
     * @param version version, de tipo {@code VersionDocumento}.
     * @param contenido contenido, de tipo {@code byte[]}.
     */
    public record DescargaVersion(VersionDocumento version, byte[] contenido) {
    }

    /** Crea el documento y guarda su primera versión. Calcula la huella SHA-256 del contenido y guarda el archivo en el almacén. Rechaza contenido vacío. */
    @Transactional
    public Documento crearDocumento(TipoDocumento tipo, String titulo, String area, LocalDate venceEn, byte[] contenido) {
        exigirContenido(contenido);
        UUID id = UUID.randomUUID();
        String ref = almacen.referencia(id, 1);
        Documento d = Documento.crear(id, tipo, titulo, area, venceEn, hoy(), sha256(contenido), ref, Instant.now(reloj));
        almacen.guardar(ref, contenido);
        Documento guardado = documentos.guardar(d);
        metricas.counter("documentos.creados").increment();
        metricas.counter("versiones.creadas").increment();
        publicarVersion(guardado, 1);
        return guardado;
    }

    /** Agrega una versión nueva. La versión anterior no se modifica: el historial siempre se conserva. */
    @Transactional
    public Documento agregarVersion(UUID id, byte[] contenido) {
        exigirContenido(contenido);
        Documento d = cargar(id);
        String hash = sha256(contenido);
        LocalDate hoy = hoy();
        d.validarNuevaVersion(hash, hoy);
        int numero = d.getVersionActual() + 1;
        String ref = almacen.referencia(id, numero);
        almacen.guardar(ref, contenido);
        d.agregarVersion(hash, ref, hoy, Instant.now(reloj));
        Documento guardado = documentos.guardar(d);
        metricas.counter("versiones.creadas").increment();
        publicarVersion(guardado, numero);
        return guardado;
    }

    /** Devuelve un documento por identificador. Si no existe, lanza RecursoNoEncontradoException. */
    @Transactional(readOnly = true)
    public Documento obtener(UUID id) {
        return cargar(id);
    }

    /** Devuelve el archivo de una versión. Antes de devolverlo verifica que su huella coincida con la guardada; si no coincide, falla. */
    @Transactional(readOnly = true)
    public DescargaVersion descargar(UUID id, int numero, String usuario) {
        Documento d = cargar(id);
        if (d.isPurgado()) {
            throw new ConflictoException("El contenido del documento fue purgado");
        }
        VersionDocumento v = d.version(numero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión no encontrada: " + numero));
        byte[] contenido = almacen.leer(v.almacenamientoRef());
        if (!sha256(contenido).equals(v.hashSha256())) {
            throw new IllegalStateException("La versión " + numero + " no pasó la verificación de integridad");
        }
        log.info("AUDITORIA documento={} version={} accion=DESCARGA usuario={}", id, numero, usuario);
        return new DescargaVersion(v, contenido);
    }

    /** Elimina el documento con un motivo, de forma lógica. Dentro del periodo de retención se rechaza. */
    @Transactional
    public Documento eliminar(UUID id, String motivo) {
        Documento d = cargar(id);
        d.eliminar(motivo, hoy(), Instant.now(reloj));
        log.info("AUDITORIA documento={} accion=ELIMINACION", id);
        return documentos.guardar(d);
    }

    /** Purga física tras la eliminación: borra el contenido de todas las versiones; metadatos y hashes permanecen. */
    @Transactional
    public Documento purgar(UUID id, String confirmacion) {
        Documento d = cargar(id);
        d.purgar(confirmacion, hoy(), Instant.now(reloj));
        for (VersionDocumento v : d.getVersiones()) {
            almacen.borrar(v.almacenamientoRef());
        }
        log.info("AUDITORIA documento={} accion=PURGA", id);
        return documentos.guardar(d);
    }

    /** Hasta 15 documentos cuyo título contiene el texto. Pide al menos 3 caracteres. */
    @Transactional(readOnly = true)
    public List<Documento> buscarPorTitulo(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.length() < 3) {
            throw new SolicitudInvalidaException("La búsqueda necesita al menos 3 caracteres");
        }
        return documentos.buscarPorTitulo(limpio, 15);
    }

    /** Lista los documentos que vencen en los próximos días. */
    @Transactional(readOnly = true)
    public List<Documento> porVencer(int dias) {
        if (dias < 0 || dias > MAX_DIAS) {
            throw new SolicitudInvalidaException("dias debe estar entre 0 y " + MAX_DIAS);
        }
        LocalDate hoy = hoy();
        return documentos.buscarVencenEntre(hoy, hoy.plusDays(dias));
    }

    /** Tarea diaria: emite alertas de permisos por vencer y marca retenciones vencidas para revisión. */
    @Transactional
    public int procesarVencimientos() {
        LocalDate hoy = hoy();
        int alertas = 0;
        for (Documento d : documentos.buscarVencenEntre(hoy, hoy.plusDays(umbralDias))) {
            if (d.necesitaAlertaVencimiento(hoy, umbralDias)) {
                d.registrarAlertaVencimiento();
                documentos.guardar(d);
                eventos.publicar(new EventoDominio("permiso-por-vencer", d.getId().toString(), Map.of(
                        "documento_id", d.getId().toString(),
                        "vence_en", d.getVenceEn().toString())));
                alertas++;
            }
        }
        for (Documento d : documentos.buscarActivosConRetencionVencida(hoy)) {
            if (d.necesitaRevisionRetencion(hoy)) {
                d.marcarRevisionRetencion();
                documentos.guardar(d);
                log.info("Retención vencida, documento marcado para revisión: {}", d.getId());
            }
        }
        return alertas;
    }

    /** Carga un documento o lanza RecursoNoEncontradoException si no existe. */
    private Documento cargar(UUID id) {
        return documentos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado: " + id));
    }

    /** Publica el evento de la nueva versión para que otros servicios lo reciban. */
    private void publicarVersion(Documento d, int numero) {
        eventos.publicar(new EventoDominio("documento-version-creada", d.getId().toString(), Map.of(
                "documento_id", d.getId().toString(),
                "numero", numero)));
    }

    /** Devuelve la fecha de hoy en la zona de México. */
    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Rechaza un contenido nulo o vacío antes de guardarlo. */
    private static void exigirContenido(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new SolicitudInvalidaException("El archivo es obligatorio y no puede estar vacío");
        }
    }

    /** Calcula la huella SHA-256 del contenido en hexadecimal. Sirve para detectar cambios en el archivo guardado. */
    static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
