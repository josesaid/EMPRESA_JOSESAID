package com.mx.josesaid.develop.documentos.api;

import com.mx.josesaid.develop.documentos.application.DocumentosService;
import com.mx.josesaid.develop.documentos.domain.Documento;
import com.mx.josesaid.develop.documentos.domain.SolicitudInvalidaException;
import com.mx.josesaid.develop.documentos.domain.TipoDocumento;
import com.mx.josesaid.develop.documentos.domain.VersionDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

/**
 * API documental. LEGAL carga, versiona, descarga y elimina; DIRECCION solo consulta.
 * Los archivos viajan en multipart: la parte "datos" es JSON y la parte "archivo" es el documento.
 */
@Validated
@RestController
@RequestMapping("/v1")
public class DocumentosControlador {

    private final DocumentosService servicio;

    /** Recibe el servicio de documentos. El controlador solo valida la entrada y delega las reglas al servicio. */
    public DocumentosControlador(DocumentosService servicio) {
        this.servicio = servicio;
    }

    /** Crea un documento con su primera versión. Solo LEGAL puede hacerlo. Responde 201 con el documento creado. */
    @PostMapping(value = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LEGAL')")
    public ResponseEntity<DocumentoRespuesta> crearDocumento(@Valid @RequestPart("datos") CrearDocumentoPeticion datos,
                                                             @RequestPart("archivo") MultipartFile archivo) {
        Documento d = servicio.crearDocumento(datos.tipo(), datos.titulo(), datos.area(), datos.venceEn(), bytes(archivo));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoRespuesta.de(d));
    }

    /** Agrega una nueva versión a un documento existente. Solo LEGAL puede hacerlo. Responde 201. */
    @PostMapping(value = "/documentos/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LEGAL')")
    public ResponseEntity<DocumentoRespuesta> agregarVersion(@PathVariable UUID id,
                                                             @RequestPart("archivo") MultipartFile archivo) {
        Documento d = servicio.agregarVersion(id, bytes(archivo));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoRespuesta.de(d));
    }

    /** Devuelve los datos de un documento sin la referencia de almacenamiento. LEGAL y DIRECCION pueden consultarlo. */
    @GetMapping("/documentos/{id}")
    @PreAuthorize("hasAnyRole('LEGAL','DIRECCION')")
    public DocumentoRespuesta obtenerDocumento(@PathVariable UUID id) {
        return DocumentoRespuesta.de(servicio.obtener(id));
    }

    /** Descarga el archivo de una versión concreta. Solo LEGAL puede hacerlo. */
    @GetMapping("/documentos/{id}/versiones/{n}/descarga")
    @PreAuthorize("hasRole('LEGAL')")
    public ResponseEntity<byte[]> descargarVersion(@PathVariable UUID id, @PathVariable int n,
                                                   Authentication autenticacion) {
        DocumentosService.DescargaVersion d = servicio.descargar(id, n, autenticacion.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documento-" + id + "-v" + n + ".bin\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(d.contenido());
    }

    /** Busca documentos cuyo título contiene el texto indicado. LEGAL y DIRECCION pueden buscar. */
    @GetMapping(value = "/documentos", params = "titulo")
    @PreAuthorize("hasAnyRole('LEGAL','DIRECCION')")
    public List<DocumentoRespuesta> buscarPorTitulo(@RequestParam @Size(max = 200) String titulo) {
        return servicio.buscarPorTitulo(titulo).stream().map(DocumentoRespuesta::de).toList();
    }

    /** Lista los documentos que vencen en los próximos días. LEGAL y DIRECCION pueden consultarlo. */
    @GetMapping("/documentos/por-vencer")
    @PreAuthorize("hasAnyRole('LEGAL','DIRECCION')")
    public List<DocumentoRespuesta> porVencer(@RequestParam int dias) {
        return servicio.porVencer(dias).stream().map(DocumentoRespuesta::de).toList();
    }

    /** Elimina un documento con un motivo. Es borrado lógico: el archivo se conserva durante el periodo de retención. Solo LEGAL puede hacerlo. */
    @PostMapping("/documentos/{id}/eliminacion")
    @PreAuthorize("hasRole('LEGAL')")
    public DocumentoRespuesta eliminarDocumento(@PathVariable UUID id, @Valid @RequestBody MotivoPeticion p) {
        return DocumentoRespuesta.de(servicio.eliminar(id, p.motivo()));
    }

    /** Purga un documento con una confirmación explícita. Solo LEGAL puede hacerlo. */
    @PostMapping("/documentos/{id}/purga")
    @PreAuthorize("hasRole('LEGAL')")
    public DocumentoRespuesta purgarDocumento(@PathVariable UUID id, @Valid @RequestBody PurgaPeticion p) {
        return DocumentoRespuesta.de(servicio.purgar(id, p.confirmacion()));
    }

    /** Devuelve el catálogo de tipos de documento. Cualquier usuario autenticado puede consultarlo. */
    @GetMapping("/catalogos/tipos-documento")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> tiposDocumento() {
        return Arrays.stream(TipoDocumento.values())
                .map(t -> Map.<String, Object>of("tipo", t.name(), "retencionAnios", t.retencionAnios()))
                .toList();
    }

    /** Lee el archivo subido en memoria. Si no se pudo leer, responde con un error de solicitud. */
    private static byte[] bytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new SolicitudInvalidaException("No se pudo leer el archivo");
        }
    }

    // ---- Peticiones ----

    /**
     * Datos del alta de un documento: tipo, título, área y fecha de vencimiento si aplica.
     *
     * @param tipo tipo o clasificación del registro, de tipo {@code TipoDocumento}.
     * @param titulo título, de tipo {@code String}.
     * @param area área de negocio a la que pertenece, de tipo {@code String}.
     * @param venceEn vence en, de tipo {@code LocalDate}.
     */
    public record CrearDocumentoPeticion(
            @NotNull TipoDocumento tipo,
            @NotBlank @Size(max = 150) String titulo,
            @NotBlank @Size(max = 100) String area,
            LocalDate venceEn) {
    }

    /**
     * Motivo de la eliminación, de hasta 300 caracteres.
     *
     * @param motivo motivo del registro o de la acción, de tipo {@code String}.
     */
    public record MotivoPeticion(@NotBlank @Size(max = 300) String motivo) {
    }

    /**
     * La confirmación debe ser el identificador del documento, tecleado por quien purga.
     *
     * @param confirmacion confirmacion, de tipo {@code String}.
     */
    public record PurgaPeticion(@NotBlank String confirmacion) {
    }

    // ---- Respuestas: nunca se expone la referencia de almacenamiento ----

    /**
     * Versión de un documento: número, huella SHA-256 del contenido y fecha de creación.
     *
     * @param numero numero, de tipo {@code int}.
     * @param hashSha256 hash sha256, de tipo {@code String}.
     * @param creadoUtc creado utc, de tipo {@code Instant}.
     */
    public record VersionRespuesta(int numero, String hashSha256, Instant creadoUtc) {
        /** Convierte una versión del dominio en su respuesta. */
        static VersionRespuesta de(VersionDocumento v) {
            return new VersionRespuesta(v.numero(), v.hashSha256(), v.creadoUtc());
        }
    }

    /**
     * Documento tal como lo ve el cliente. Nunca incluye la referencia de almacenamiento.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param tipo tipo o clasificación del registro, de tipo {@code TipoDocumento}.
     * @param titulo título, de tipo {@code String}.
     * @param area área de negocio a la que pertenece, de tipo {@code String}.
     * @param venceEn vence en, de tipo {@code LocalDate}.
     * @param retencionHasta retencion hasta, de tipo {@code LocalDate}.
     * @param versionActual version actual, de tipo {@code int}.
     * @param eliminado eliminado, de tipo {@code boolean}.
     * @param purgado purgado, de tipo {@code boolean}.
     * @param versiones versiones, de tipo {@code List<VersionRespuesta>}.
     */
    public record DocumentoRespuesta(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                                     LocalDate retencionHasta, int versionActual, boolean eliminado,
                                     boolean purgado, List<VersionRespuesta> versiones) {
        /** Convierte un documento del dominio en su respuesta, sin la referencia de almacenamiento. */
        static DocumentoRespuesta de(Documento d) {
            return new DocumentoRespuesta(d.getId(), d.getTipo(), d.getTitulo(), d.getArea(), d.getVenceEn(),
                    d.getRetencionHasta(), d.getVersionActual(), d.isEliminado(), d.isPurgado(),
                    d.getVersiones().stream().map(VersionRespuesta::de).toList());
        }
    }
}
