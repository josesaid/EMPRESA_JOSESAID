package com.mx.josesaid.develop.compras.api;

import com.mx.josesaid.develop.compras.domain.AprobacionNoPermitidaException;
import com.mx.josesaid.develop.compras.domain.ConflictoException;
import com.mx.josesaid.develop.compras.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.compras.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.compras.domain.SolicitudInvalidaException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores RFC 9457 con type {@code urn:josesaid:compras-proveedores:error:<slug>}. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final String PREFIJO = "urn:josesaid:compras-proveedores:error:";

    /** Convierte un recurso inexistente en un problema HTTP. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado", ex.getMessage());
    }

    /** Convierte un intento de aprobación no permitido en un problema HTTP. */
    @ExceptionHandler(AprobacionNoPermitidaException.class)
    ProblemDetail aprobacionNoPermitida(AprobacionNoPermitidaException ex) {
        return problema(HttpStatus.FORBIDDEN, "aprobacion-no-permitida", ex.getMessage());
    }

    /** Convierte un conflicto de estado en un problema HTTP. */
    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto", ex.getMessage());
    }

    /** Convierte un conflicto de concurrencia en un problema HTTP. El cliente debe reintentar. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrencia(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto-concurrencia",
                "El registro cambió mientras se procesaba. Vuelva a intentarlo.");
    }

    /** Convierte una regla de negocio incumplida en un problema HTTP. */
    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", ex.getMessage());
    }

    /** Convierte una solicitud inválida en un problema HTTP. */
    @ExceptionHandler(SolicitudInvalidaException.class)
    ProblemDetail solicitudInvalida(SolicitudInvalidaException ex) {
        return problema(HttpStatus.BAD_REQUEST, "datos-invalidos", ex.getMessage());
    }

    /** Convierte errores de validación en un problema HTTP con el detalle de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "datos-invalidos", detalle);
    }

    /** Construye la respuesta de error con su estatus, código y detalle. */
    private static ProblemDetail problema(HttpStatus estatus, String slug, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estatus, detalle);
        pd.setType(URI.create(PREFIJO + slug));
        pd.setTitle(estatus.getReasonPhrase());
        return pd;
    }

    /** Convierte parámetros inválidos en un problema HTTP. */
    @org.springframework.web.bind.annotation.ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    org.springframework.http.ProblemDetail parametrosInvalidos(jakarta.validation.ConstraintViolationException e) {
        return org.springframework.http.ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Revise los parámetros indicados");
    }
}
