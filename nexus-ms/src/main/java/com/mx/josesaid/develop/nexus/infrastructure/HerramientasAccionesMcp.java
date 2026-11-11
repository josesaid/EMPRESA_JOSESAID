package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mx.josesaid.develop.nexus.application.AccionNoEncontradaException;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.ConfirmacionAcciones;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/** Confirma o cancela acciones preparadas por MCP. Solo quien las preparó puede actuar sobre ellas. */
public final class HerramientasAccionesMcp {

    private final ConfirmacionAcciones confirmaciones;
    private final AccionesPendientes acciones;

    /** Recibe el servicio de confirmación y el registro de acciones para las herramientas MCP. */
    public HerramientasAccionesMcp(ConfirmacionAcciones confirmaciones, AccionesPendientes acciones) {
        this.confirmaciones = confirmaciones;
        this.acciones = acciones;
    }

    /** Herramienta MCP: confirma una acción pendiente con su código. Un código incorrecto cancela la acción. */
    @Tool(name = "confirmar_accion",
          description = "Confirma una acción pendiente con su identificador y su código de confirmación. "
                  + "Solo funciona para la persona que la preparó y dentro de su vigencia. "
                  + "Un código incorrecto cancela la acción.")
    public String confirmarAccion(
            @ToolParam(description = "Identificador de la acción preparada", required = true) String accionId,
            @ToolParam(description = "Código de confirmación de 8 dígitos que devolvió la preparación", required = true) String codigo) {
        RegistroHerramientasUsadas.registrar("confirmar_accion");
        return confirmaciones.confirmarConCodigo(UUID.fromString(accionId), usuarioActual(), codigo);
    }

    /** Herramienta MCP: cancela una acción pendiente del usuario. */
    @Tool(name = "cancelar_accion",
          description = "Cancela una acción pendiente sin ejecutarla. Solo funciona para la persona que la preparó.")
    public String cancelarAccion(
            @ToolParam(description = "Identificador de la acción preparada", required = true) String accionId) {
        RegistroHerramientasUsadas.registrar("cancelar_accion");
        acciones.cancelar(UUID.fromString(accionId), usuarioActual());
        return "Acción " + accionId + " cancelada.";
    }

    /** Herramienta MCP: devuelve el estado de una acción pendiente del usuario. */
    @Tool(name = "consultar_estado_accion",
          description = "Devuelve el estado de una acción preparada por la persona actual: PENDIENTE, CONFIRMADA o "
                  + "EXPIRADA, con su resumen y su vencimiento. Solo funciona para quien la preparó.")
    public String consultarEstadoAccion(
            @ToolParam(description = "Identificador de la acción preparada", required = true) String accionId) {
        RegistroHerramientasUsadas.registrar("consultar_estado_accion");
        UUID id;
        try {
            id = UUID.fromString(accionId.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            return "El identificador de la acción no es válido.";
        }
        AccionPendiente accion;
        try {
            accion = acciones.consultar(id, usuarioActual());
        } catch (AccionNoEncontradaException e) {
            return "No encontré esa acción.";
        }
        return FiltroDatosSensibles.redactar("Acción " + accion.id() + ": estatus " + accion.estatus()
                + ". Resumen: " + accion.resumen() + ". Vence: " + accion.expiraUtc() + ".").texto();
    }

    /** Devuelve el nombre del usuario autenticado. */
    private static String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
