package com.mx.josesaid.develop.nexus.application;

import java.util.UUID;

/**
 * Confirmación desde MCP: exige el código de una sola vez además de la identidad del usuario.
 * Un código incorrecto cancela la acción: no se permiten intentos repetidos contra la misma acción.
 */
public final class ConfirmacionAcciones {

    private final AccionesPendientes acciones;
    private final CodigoConfirmacion codigos;

    public ConfirmacionAcciones(AccionesPendientes acciones, CodigoConfirmacion codigos) {
        this.acciones = acciones;
        this.codigos = codigos;
    }

    public String confirmarConCodigo(UUID id, String usuario, String codigo) {
        AccionPendiente accion = acciones.consultar(id, usuario);
        if (!AccionPendiente.PENDIENTE.equals(accion.estatus())) {
            throw new AccionNoPendienteException("La acción ya está " + accion.estatus().toLowerCase());
        }
        if (!codigos.coincide(id, usuario, codigo)) {
            acciones.cancelar(id, usuario);
            throw new CodigoConfirmacionInvalidoException(
                    "Código incorrecto. La acción se canceló por seguridad; pide a Nexus que la prepare de nuevo");
        }
        return acciones.confirmar(id, usuario);
    }
}
