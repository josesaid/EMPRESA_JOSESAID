package com.mx.josesaid.develop.nexus.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.DatosAlta;
import com.mx.josesaid.develop.nexus.application.AccionNoEncontradaException;
import com.mx.josesaid.develop.nexus.application.AccionNoPendienteException;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacionInvalidoException;
import com.mx.josesaid.develop.nexus.application.ConfirmacionAcciones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.AuditoriaConsultas;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;
import com.mx.josesaid.develop.nexus.application.PresupuestoTokens;
import com.mx.josesaid.develop.nexus.application.RegistroConsulta;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/** Endpoints del asistente Nexus: consultas, confirmación de acciones y administración de presupuesto. Todos requieren un JWT válido. */
@RestController
@RequestMapping("/nexus")
@Validated
public class NexusControlador {

    private final AsistenteIA asistente;
    private final AuditoriaConsultas auditoria;
    private final AccionesPendientes acciones;
    private final PresupuestoTokens presupuesto;
    private final ConfirmacionAcciones confirmacion;

    /** Recibe el asistente, la auditoría, el registro de acciones, el control de presupuesto de tokens y la confirmación de acciones. */
    public NexusControlador(AsistenteIA asistente, AuditoriaConsultas auditoria, AccionesPendientes acciones,
                            PresupuestoTokens presupuesto, ConfirmacionAcciones confirmacion) {
        this.asistente = asistente;
        this.auditoria = auditoria;
        this.acciones = acciones;
        this.presupuesto = presupuesto;
        this.confirmacion = confirmacion;
    }

    /** El usuario confirma una acción que Nexus preparó. Solo quien la preparó puede confirmarla. */
    @PostMapping("/acciones/{id}/confirmacion")
    @PreAuthorize("hasAnyRole('VENTAS','CONTADOR')")
    public ResponseEntity<String> confirmarAccion(@PathVariable UUID id,
                                                  @RequestBody @Valid ConfirmacionSolicitud solicitud,
                                                  @AuthenticationPrincipal Jwt jwt) {
        String respuesta = confirmacion.confirmarConCodigo(id, jwt.getSubject(), solicitud.codigo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(respuesta);
    }

    /** Datos del alta pendiente (sin contraseña) para que la pantalla de altas los muestre y envíe. */
    @GetMapping("/acciones/{id}/alta")
    @PreAuthorize("hasRole('ADMIN')")
    public DatosAlta datosAlta(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return DatosAlta.de(acciones.altaVigente(id, jwt.getSubject()).payload());
    }

    /** El administrador verifica el código de un alta antes de crear el usuario. Código incorrecto cancela la acción. */
    @PostMapping("/acciones/{id}/alta/verificacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> verificarAlta(@PathVariable UUID id,
                                              @RequestBody @Valid ConfirmacionSolicitud solicitud,
                                              @AuthenticationPrincipal Jwt jwt) {
        confirmacion.verificarAlta(id, jwt.getSubject(), solicitud.codigo());
        return ResponseEntity.noContent().build();
    }

    /** Marca el alta como completada después de que la pantalla de altas creó el usuario. */
    @PostMapping("/acciones/{id}/alta/completacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> completarAlta(@PathVariable UUID id,
                                              @RequestBody @Valid ConfirmacionSolicitud solicitud,
                                              @AuthenticationPrincipal Jwt jwt) {
        confirmacion.completarAlta(id, jwt.getSubject(), solicitud.codigo());
        return ResponseEntity.noContent().build();
    }

    /** Muestra al dueño la acción pendiente antes de confirmarla. */
    @GetMapping("/acciones/{id}")
    @PreAuthorize("hasAnyRole('VENTAS','CONTADOR','ADMIN')")
    public AccionResumen consultarAccion(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var accion = acciones.consultar(id, jwt.getSubject());
        return new AccionResumen(accion.id(), accion.tipo(), accion.resumen(), accion.estatus(), accion.expiraUtc());
    }

    /** El usuario descarta una acción pendiente sin ejecutarla. */
    @PostMapping("/acciones/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('VENTAS','CONTADOR')")
    public ResponseEntity<Void> cancelarAccion(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        acciones.cancelar(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /**
     * Código de confirmación de 8 dígitos que el usuario escribe para ejecutar una acción pendiente.
     *
     * @param codigo código legible que identifica el registro, de tipo {@code String}.
     */
    public record ConfirmacionSolicitud(@NotBlank @Pattern(regexp = "\\d{8}") String codigo) {
    }

    /** Responde 422 cuando el código de confirmación no coincide o ya no sirve. */
    @ExceptionHandler(CodigoConfirmacionInvalidoException.class)
    ResponseEntity<String> codigoInvalido(CodigoConfirmacionInvalidoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
    }

    /** Responde 409 cuando la acción ya fue confirmada, cancelada o expiró. */
    @ExceptionHandler(AccionNoPendienteException.class)
    ResponseEntity<String> noPendiente(AccionNoPendienteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    /** Responde 404 cuando la acción no existe o es de otro usuario. */
    @ExceptionHandler(AccionNoEncontradaException.class)
    ResponseEntity<String> noEncontrada(AccionNoEncontradaException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    /** Responde una consulta en lenguaje natural. Antes de llamar al modelo verifica el presupuesto de tokens, y después registra la consulta en auditoría. Sus herramientas solo preparan acciones; nunca las ejecutan. */
    @PostMapping("/consultas")
    @PreAuthorize("isAuthenticated()")
    public ConsultaResponse consultar(@RequestBody @Valid ConsultaRequest solicitud,
                                      @AuthenticationPrincipal Jwt jwt) {
        String usuario = jwt.getSubject();
        String usuarioAuditoria = jwt.getClaimAsString("correo");
        if (usuarioAuditoria == null || usuarioAuditoria.isBlank()) {
            usuarioAuditoria = usuario;
        }
        presupuesto.exigirDisponible(usuarioAuditoria);
        var respuesta = asistente.consultar(new PreguntaAsistente(usuario, solicitud.pregunta()));
        String texto = respuesta.texto() == null ? "" : respuesta.texto();
        String resumen = FiltroDatosSensibles.redactar(texto.length() > 500 ? texto.substring(0, 500) : texto).texto();
        String herramientas = String.join(",", respuesta.herramientas());
        if (herramientas.length() > 500) {
            herramientas = herramientas.substring(0, 500);
        }
        auditoria.registrar(new RegistroConsulta(
                usuarioAuditoria,
                FiltroDatosSensibles.redactar(solicitud.pregunta()).texto(),
                resumen,
                herramientas.isEmpty() ? null : herramientas,
                respuesta.tokensEntrada(),
                respuesta.tokensSalida(),
                Instant.now()));
        return new ConsultaResponse(FiltroDatosSensibles.redactar(texto).texto());
    }
}
