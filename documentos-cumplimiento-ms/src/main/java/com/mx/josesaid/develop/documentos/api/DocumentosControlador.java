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

/**
 * API documental. LEGAL carga, versiona, descarga y elimina; DIRECCION solo consulta.
 * Los archivos viajan en multipart: la parte "datos" es JSON y la parte "archivo" es el documento.
 */
@RestController
@RequestMapping("/v1")
public class DocumentosControlador {

    private final DocumentosService servicio;

    public DocumentosControlador(DocumentosService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(value = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LEGAL')")
    public ResponseEntity<DocumentoRespuesta> crearDocumento(@Valid @RequestPart("datos") CrearDocumentoPeticion datos,
                                                             @RequestPart("archivo") MultipartFile archivo) {
        Documento d = servicio.crearDocumento(datos.tipo(), datos.titulo(), datos.area(), datos.venceEn(), bytes(archivo));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoRespuesta.de(d));
    }

    @PostMapping(value = "/documentos/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LEGAL')")
    public ResponseEntity<DocumentoRespuesta> agregarVersion(@PathVariable UUID id,
                                                             @RequestPart("archivo") MultipartFile archivo) {
        Documento d = servicio.agregarVersion(id, bytes(archivo));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentoRespuesta.de(d));
    }

    @GetMapping("/documentos/{id}")
    @PreAuthorize("hasAnyRole('LEGAL','DIRECCION')")
    public DocumentoRespuesta obtenerDocumento(@PathVariable UUID id) {
        return DocumentoRespuesta.de(servicio.obtener(id));
    }

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

    @GetMapping("/documentos/por-vencer")
    @PreAuthorize("hasAnyRole('LEGAL','DIRECCION')")
    public List<DocumentoRespuesta> porVencer(@RequestParam int dias) {
        return servicio.porVencer(dias).stream().map(DocumentoRespuesta::de).toList();
    }

    @PostMapping("/documentos/{id}/eliminacion")
    @PreAuthorize("hasRole('LEGAL')")
    public DocumentoRespuesta eliminarDocumento(@PathVariable UUID id, @Valid @RequestBody MotivoPeticion p) {
        return DocumentoRespuesta.de(servicio.eliminar(id, p.motivo()));
    }

    @GetMapping("/catalogos/tipos-documento")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> tiposDocumento() {
        return Arrays.stream(TipoDocumento.values())
                .map(t -> Map.<String, Object>of("tipo", t.name(), "retencionAnios", t.retencionAnios()))
                .toList();
    }

    private static byte[] bytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new SolicitudInvalidaException("No se pudo leer el archivo");
        }
    }

    // ---- Peticiones ----

    public record CrearDocumentoPeticion(
            @NotNull TipoDocumento tipo,
            @NotBlank @Size(max = 150) String titulo,
            @NotBlank @Size(max = 100) String area,
            LocalDate venceEn) {
    }

    public record MotivoPeticion(@NotBlank @Size(max = 300) String motivo) {
    }

    // ---- Respuestas: nunca se expone la referencia de almacenamiento ----

    public record VersionRespuesta(int numero, String hashSha256, Instant creadoUtc) {
        static VersionRespuesta de(VersionDocumento v) {
            return new VersionRespuesta(v.numero(), v.hashSha256(), v.creadoUtc());
        }
    }

    public record DocumentoRespuesta(UUID id, TipoDocumento tipo, String titulo, String area, LocalDate venceEn,
                                     LocalDate retencionHasta, int versionActual, boolean eliminado,
                                     List<VersionRespuesta> versiones) {
        static DocumentoRespuesta de(Documento d) {
            return new DocumentoRespuesta(d.getId(), d.getTipo(), d.getTitulo(), d.getArea(), d.getVenceEn(),
                    d.getRetencionHasta(), d.getVersionActual(), d.isEliminado(),
                    d.getVersiones().stream().map(VersionRespuesta::de).toList());
        }
    }
}
