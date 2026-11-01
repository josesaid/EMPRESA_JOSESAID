package com.mx.josesaid.develop.nexus.api;

import java.net.SocketTimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import com.mx.josesaid.develop.nexus.application.PresupuestoAgotadoException;

/** Errores de la API con formato de problema (RFC 9457). */
@RestControllerAdvice
public class ManejoErrores {

    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail tiempoAgotado(ResourceAccessException e) {
        return problema(HttpStatus.GATEWAY_TIMEOUT, "tiempo-agotado",
                "El modelo o un servicio no respondió a tiempo. Intenta de nuevo.");
    }

    @ExceptionHandler(SocketTimeoutException.class)
    ProblemDetail tiempoAgotadoSocket(SocketTimeoutException e) {
        return problema(HttpStatus.GATEWAY_TIMEOUT, "tiempo-agotado",
                "El modelo o un servicio no respondió a tiempo. Intenta de nuevo.");
    }

    @ExceptionHandler(PresupuestoAgotadoException.class)
    ProblemDetail presupuestoAgotado(PresupuestoAgotadoException e) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "presupuesto-agotado", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String tipo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setType(java.net.URI.create("https://empresa-josesaid/errores/" + tipo));
        p.setTitle(estado.getReasonPhrase());
        return p;
    }
}
