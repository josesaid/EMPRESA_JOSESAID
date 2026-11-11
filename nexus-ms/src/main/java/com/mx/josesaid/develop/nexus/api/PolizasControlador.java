package com.mx.josesaid.develop.nexus.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;
import com.mx.josesaid.develop.nexus.application.ServicioPolizas;
import com.mx.josesaid.develop.nexus.application.SugerenciaPolizaInvalidaException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sugerencias de pólizas. Nexus solo prepara la acción: la póliza se registra cuando el contador
 * la confirma con POST /nexus/acciones/{id}/confirmacion.
 */
@RestController
@RequestMapping("/nexus/polizas")
@Validated
public class PolizasControlador {

    private final ServicioPolizas servicio;
    private final CodigoConfirmacion codigos;

    /** Recibe el servicio que prepara las sugerencias y el generador de códigos de confirmación. */
    public PolizasControlador(ServicioPolizas servicio, CodigoConfirmacion codigos) {
        this.servicio = servicio;
        this.codigos = codigos;
    }

    /** Prepara una sugerencia de póliza a partir de la descripción del contador. Devuelve la acción pendiente junto con su código de confirmación de una sola vez. Solo CONTADOR puede llamarlo. */
    @PostMapping("/sugerencias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTADOR')")
    public SugerenciaRespuesta sugerir(@RequestBody @Valid SugerenciaPolizaSolicitud solicitud,
                                       @AuthenticationPrincipal Jwt jwt) {
        AccionPendiente accion = servicio.preparar(jwt.getSubject(), solicitud.descripcion());
        return new SugerenciaRespuesta(accion.id(), accion.tipo(), accion.resumen(), accion.estatus(),
                accion.expiraUtc(), codigos.generar(accion.id(), accion.usuario()));
    }

    /** Responde 422 cuando la sugerencia no es válida, con el motivo en el campo error. */
    @ExceptionHandler(SugerenciaPolizaInvalidaException.class)
    ResponseEntity<Map<String, String>> sugerenciaInvalida(SugerenciaPolizaInvalidaException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
    }

    /**
     * Cuerpo de la petición: descripción de la póliza, de 1 a 500 caracteres.
     *
     * @param descripcion componente de tipo {@code String}.
     */
    public record SugerenciaPolizaSolicitud(@NotBlank @Size(max = 500) String descripcion) {
    }

    /**
     * Respuesta de una sugerencia. Incluye el código de confirmación de una sola vez, que solo recibe
     * quien pidió la sugerencia (CONTADOR). Sin el código la acción no puede confirmarse.
      * @param id componente de tipo {@code UUID}.
      * @param tipo componente de tipo {@code String}.
      * @param resumen componente de tipo {@code String}.
      * @param estatus componente de tipo {@code String}.
      * @param expiraUtc componente de tipo {@code Instant}.
      * @param codigoConfirmacion componente de tipo {@code String}.
     */
    public record SugerenciaRespuesta(UUID id, String tipo, String resumen, String estatus,
                                      Instant expiraUtc, String codigoConfirmacion) {
    }
}
