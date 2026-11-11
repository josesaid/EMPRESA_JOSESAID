package com.mx.josesaid.develop.usuarios_accesos.api;

import com.mx.josesaid.develop.usuarios_accesos.domain.BloqueoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.CambioPasswordRequeridoException;
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

    /** Convierte credenciales incorrectas en un problema HTTP. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credenciales(CredencialesInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "credenciales-invalidas", ex.getMessage());
    }

    /** Convierte una contraseña temporal pendiente de cambio en un problema HTTP. */
    @ExceptionHandler(CambioPasswordRequeridoException.class)
    ProblemDetail cambioRequerido(CambioPasswordRequeridoException ex) {
        return problema(HttpStatus.FORBIDDEN, "cambio-password-requerido", ex.getMessage());
    }

    /** Convierte una cuenta bloqueada por intentos fallidos en un problema HTTP. */
    @ExceptionHandler(BloqueoException.class)
    ProblemDetail bloqueo(BloqueoException ex) {
        return problema(HttpStatus.LOCKED, "cuenta-bloqueada", ex.getMessage());
    }

    /** Convierte un recurso inexistente en un problema HTTP. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado", ex.getMessage());
    }

    /** Convierte un conflicto de estado en un problema HTTP. */
    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto", ex.getMessage());
    }

    /** Convierte una regla de negocio incumplida en un problema HTTP. */
    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", ex.getMessage());
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
}
