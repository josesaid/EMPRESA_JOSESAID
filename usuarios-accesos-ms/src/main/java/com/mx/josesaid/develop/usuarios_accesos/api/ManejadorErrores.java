package com.mx.josesaid.develop.usuarios_accesos.api;

import com.mx.josesaid.develop.usuarios_accesos.domain.BloqueoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.ConflictoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.CredencialesInvalidasException;
import com.mx.josesaid.develop.usuarios_accesos.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.ReglaDeNegocioException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones a respuestas RFC 9457 (ProblemDetail).
 *
 * <p>Cada error tiene un {@code type} estable con el prefijo {@code urn:josesaid:usuarios-accesos:error:}.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final String PREFIJO = "urn:josesaid:usuarios-accesos:error:";

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credenciales(CredencialesInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", ex.getMessage());
    }

    @ExceptionHandler(BloqueoException.class)
    ProblemDetail bloqueo(BloqueoException ex) {
        return problema(HttpStatus.LOCKED, "cuenta-bloqueada", ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto", ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "datos-invalidos", detalle);
    }

    private static ProblemDetail problema(HttpStatus estatus, String slug, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estatus, detalle);
        pd.setType(URI.create(PREFIJO + slug));
        pd.setTitle(estatus.getReasonPhrase());
        return pd;
    }
}
