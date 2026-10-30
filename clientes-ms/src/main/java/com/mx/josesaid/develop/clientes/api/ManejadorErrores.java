package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.application.ConflictoRfcException;
import com.mx.josesaid.develop.clientes.application.DuplicadoPosibleException;
import com.mx.josesaid.develop.clientes.application.EntidadNoEncontradaException;
import com.mx.josesaid.develop.clientes.application.ReglaNegocioException;
import com.mx.josesaid.develop.clientes.domain.TransicionInvalidaException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte excepciones en respuestas RFC 9457 (application/problem+json).
 * Los tipos son URN estables: urn:clientes:error:<codigo>.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(EntidadNoEncontradaException.class)
    ProblemDetail noEncontrado(EntidadNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "recurso-no-encontrado", "Recurso no encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflictoRfcException.class)
    ProblemDetail rfcDuplicado(ConflictoRfcException e) {
        ProblemDetail pd = problema(HttpStatus.CONFLICT, "rfc-duplicado", "Conflicto de RFC", e.getMessage());
        pd.setProperty("clienteExistenteId", e.getClienteExistenteId());
        return pd;
    }

    @ExceptionHandler(DuplicadoPosibleException.class)
    ProblemDetail posibleDuplicado(DuplicadoPosibleException e) {
        ProblemDetail pd = problema(HttpStatus.CONFLICT, "posible-duplicado", "Posible duplicado",
                "Confirme la alta con ?confirmarDuplicado=true");
        pd.setProperty("coincidencias", e.getCoincidencias());
        return pd;
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "transicion-invalida", "Transición no permitida", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", "Regla de negocio", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "conflicto-de-integridad", "Conflicto de integridad",
                "La operación viola una restricción única; reintente consultando el registro existente");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        List<Map<String, String>> errores = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("campo", f.getField(), "mensaje", String.valueOf(f.getDefaultMessage())))
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacion", "Datos inválidos",
                "Revise los campos indicados");
        pd.setProperty("errores", errores);
        return pd;
    }

    private static ProblemDetail problema(HttpStatus estado, String codigo, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setType(URI.create("urn:clientes:error:" + codigo));
        pd.setTitle(titulo);
        return pd;
    }
}
