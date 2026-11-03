package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.ConfirmacionAcciones;

/** Confirma o cancela acciones preparadas por MCP. Solo quien las preparó puede actuar sobre ellas. */
public final class HerramientasAccionesMcp {

    private final ConfirmacionAcciones confirmaciones;
    private final AccionesPendientes acciones;

    public HerramientasAccionesMcp(ConfirmacionAcciones confirmaciones, AccionesPendientes acciones) {
        this.confirmaciones = confirmaciones;
        this.acciones = acciones;
    }

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

    @Tool(name = "cancelar_accion",
          description = "Cancela una acción pendiente sin ejecutarla. Solo funciona para la persona que la preparó.")
    public String cancelarAccion(
            @ToolParam(description = "Identificador de la acción preparada", required = true) String accionId) {
        RegistroHerramientasUsadas.registrar("cancelar_accion");
        acciones.cancelar(UUID.fromString(accionId), usuarioActual());
        return "Acción " + accionId + " cancelada.";
    }

    private static String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
