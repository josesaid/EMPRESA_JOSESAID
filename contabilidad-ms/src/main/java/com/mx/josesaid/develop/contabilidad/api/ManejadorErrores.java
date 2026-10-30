package com.mx.josesaid.develop.contabilidad.api;

import com.mx.josesaid.develop.contabilidad.domain.ConflictoException;
import com.mx.josesaid.develop.contabilidad.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.contabilidad.domain.ReglaDeNegocioException;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores RFC 9457 con type estable {@code urn:josesaid:contabilidad:error:<slug>}. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final String PREFIJO = "urn:josesaid:contabilidad:error:";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "no-encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrencia(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "conflicto-concurrencia",
                "El registro cambió mientras se procesaba. Vuelva a intentarlo.");
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

    @ExceptionHandler(DateTimeParseException.class)
    ProblemDetail fechaInvalida(DateTimeParseException ex) {
        return problema(HttpStatus.BAD_REQUEST, "datos-invalidos", "Formato de periodo inválido. Use AAAA-MM");
    }

    private static ProblemDetail problema(HttpStatus estatus, String slug, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estatus, detalle);
        pd.setType(URI.create(PREFIJO + slug));
        pd.setTitle(estatus.getReasonPhrase());
        return pd;
    }
}
