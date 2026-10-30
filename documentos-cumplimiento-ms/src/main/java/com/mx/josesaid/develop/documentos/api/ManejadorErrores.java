package com.mx.josesaid.develop.documentos.api;

import com.mx.josesaid.develop.documentos.domain.ConflictoException;
import com.mx.josesaid.develop.documentos.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.documentos.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.documentos.domain.SolicitudInvalidaException;
import java.net.URI;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Convierte excepciones en respuestas RFC 9457 (application/problem+json). */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ReglaDeNegocioException.class)
    ResponseEntity<ProblemDetail> reglaDeNegocio(ReglaDeNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "recurso-no-encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ProblemDetail> conflicto(ConflictoException e) {
        return respuesta(HttpStatus.CONFLICT, "conflicto-de-estado", e.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    ResponseEntity<ProblemDetail> solicitudInvalida(SolicitudInvalidaException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "solicitud-invalida", e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ProblemDetail> parteFaltante(MissingServletRequestPartException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "solicitud-invalida",
                "Falta la parte multipart requerida: " + e.getRequestPartName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ProblemDetail> archivoGrande(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.CONTENT_TOO_LARGE, "archivo-demasiado-grande",
                "El archivo supera el tamaño máximo permitido");
    }

    /** Dos usuarios modificaron el mismo documento: el segundo recibe 409 y debe volver a leer. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> concurrencia(OptimisticLockingFailureException e) {
        return respuesta(HttpStatus.CONFLICT, "conflicto-concurrencia",
                "El documento cambió mientras se procesaba. Vuelva a leerlo e intente de nuevo.");
    }

    private static ResponseEntity<ProblemDetail> respuesta(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(estado.getReasonPhrase());
        problema.setType(URI.create("urn:josesaid:documentos-cumplimiento:error:" + slug));
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }
}
