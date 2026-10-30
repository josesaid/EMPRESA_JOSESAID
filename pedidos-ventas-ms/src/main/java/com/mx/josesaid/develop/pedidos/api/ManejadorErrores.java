package com.mx.josesaid.develop.pedidos.api;

import com.mx.josesaid.develop.pedidos.domain.ConflictoException;
import com.mx.josesaid.develop.pedidos.domain.CotizacionVencidaException;
import com.mx.josesaid.develop.pedidos.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.pedidos.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.pedidos.domain.SolicitudInvalidaException;
import java.net.URI;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(CotizacionVencidaException.class)
    ResponseEntity<ProblemDetail> cotizacionVencida(CotizacionVencidaException e) {
        return respuesta(HttpStatus.GONE, "cotizacion-vencida", e.getMessage());
    }

    /** Dos usuarios modificaron el mismo registro: el segundo recibe 409 y debe volver a leer. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> concurrencia(OptimisticLockingFailureException e) {
        return respuesta(HttpStatus.CONFLICT, "conflicto-concurrencia",
                "El registro cambió mientras se procesaba. Vuelva a leerlo e intente de nuevo.");
    }

    private static ResponseEntity<ProblemDetail> respuesta(HttpStatus estado, String slug, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(estado.getReasonPhrase());
        problema.setType(URI.create("urn:josesaid:pedidos-ventas:error:" + slug));
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }
}
