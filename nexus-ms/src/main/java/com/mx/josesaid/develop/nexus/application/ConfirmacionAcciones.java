package com.mx.josesaid.develop.nexus.application;

import java.util.UUID;

/**
 * Confirmación desde MCP: exige el código de una sola vez además de la identidad del usuario.
 * Un código incorrecto cancela la acción: no se permiten intentos repetidos contra la misma acción.
 */
public final class ConfirmacionAcciones {

    private final AccionesPendientes acciones;
    private final CodigoConfirmacion codigos;

    /** Recibe el registro de acciones y el verificador de códigos. */
    public ConfirmacionAcciones(AccionesPendientes acciones, CodigoConfirmacion codigos) {
        this.acciones = acciones;
        this.codigos = codigos;
    }

    /** Confirma una acción pidiendo el código de confirmación. Primero verifica que esté pendiente y que el código coincida. Solo si todo es válido la ejecuta. */
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

    /** Verifica el código de un alta sin crear nada. Código incorrecto cancela la acción por seguridad. */
    public void verificarAlta(UUID id, String usuario, String codigo) {
        acciones.altaVigente(id, usuario);
        if (!codigos.coincide(id, usuario, codigo)) {
            acciones.cancelar(id, usuario);
            throw new CodigoConfirmacionInvalidoException(
                    "Código incorrecto. La acción se canceló por seguridad; pide a Nexus que la prepare de nuevo");
        }
    }

    /** Marca el alta como completada tras crear el usuario. Vuelve a verificar el código. */
    public void completarAlta(UUID id, String usuario, String codigo) {
        verificarAlta(id, usuario, codigo);
        acciones.completarAlta(id, usuario);
    }
}
