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

    /** Convierte un tiempo agotado al llamar al modelo o a otro servicio en un 504 con formato RFC 9457. */
    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail tiempoAgotado(ResourceAccessException e) {
        return problema(HttpStatus.GATEWAY_TIMEOUT, "tiempo-agotado",
                "El modelo o un servicio no respondió a tiempo. Intenta de nuevo.");
    }

    /** Igual que el anterior, para el tiempo agotado de la conexión por socket. */
    @ExceptionHandler(SocketTimeoutException.class)
    ProblemDetail tiempoAgotadoSocket(SocketTimeoutException e) {
        return problema(HttpStatus.GATEWAY_TIMEOUT, "tiempo-agotado",
                "El modelo o un servicio no respondió a tiempo. Intenta de nuevo.");
    }

    /** Convierte el agotamiento del presupuesto de tokens en un 429. El mensaje describe qué presupuesto se agotó. */
    @ExceptionHandler(PresupuestoAgotadoException.class)
    ProblemDetail presupuestoAgotado(PresupuestoAgotadoException e) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "presupuesto-agotado", e.getMessage());
    }

    /** Construye la respuesta de error con el tipo, el título del estado HTTP y el detalle indicados. */
    private static ProblemDetail problema(HttpStatus estado, String tipo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setType(java.net.URI.create("https://empresa-josesaid/errores/" + tipo));
        p.setTitle(estado.getReasonPhrase());
        return p;
    }
}
