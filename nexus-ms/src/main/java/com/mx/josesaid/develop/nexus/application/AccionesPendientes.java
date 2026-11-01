package com.mx.josesaid.develop.nexus.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Prepara y confirma acciones de escritura. Nexus nunca ejecuta una acción por sí solo:
 * la confirmación la hace el usuario con su propia sesión, y solo quien preparó la acción puede confirmarla.
 */
public final class AccionesPendientes {

    public static final String TIPO_PEDIDO_BORRADOR = "PEDIDO_BORRADOR";
    public static final String TIPO_PROSPECTO_BORRADOR = "PROSPECTO_BORRADOR";
    public static final String TIPO_PEDIDO_CONFIRMAR = "PEDIDO_CONFIRMAR";
    public static final String TIPO_PEDIDO_CANCELAR = "PEDIDO_CANCELAR";
    public static final Duration VIGENCIA = Duration.ofMinutes(10);

    private final AlmacenAcciones almacen;
    private final EjecutorAcciones ejecutor;
    private final Clock reloj;

    public AccionesPendientes(AlmacenAcciones almacen, EjecutorAcciones ejecutor, Clock reloj) {
        this.almacen = almacen;
        this.ejecutor = ejecutor;
        this.reloj = reloj;
    }

    public AccionPendiente preparar(String usuario, String tipo, String payload, String resumen) {
        Instant ahora = reloj.instant();
        AccionPendiente accion = new AccionPendiente(UUID.randomUUID(), usuario, tipo, payload, resumen,
                AccionPendiente.PENDIENTE, ahora, ahora.plus(VIGENCIA));
        almacen.guardar(accion);
        return accion;
    }

    /**
     * Confirma y ejecuta la acción. Si la ejecución falla, la acción sigue pendiente
     * para que el usuario pueda intentarlo de nuevo mientras no expire.
     */
    public String confirmar(UUID id, String usuario) {
        AccionPendiente accion = propia(id, usuario);
        if (!AccionPendiente.PENDIENTE.equals(accion.estatus())) {
            throw new AccionNoPendienteException("La acción ya está " + accion.estatus().toLowerCase());
        }
        if (reloj.instant().isAfter(accion.expiraUtc())) {
            almacen.guardar(accion.conEstatus(AccionPendiente.EXPIRADA));
            throw new AccionNoPendienteException("La acción expiró; pide a Nexus que la prepare de nuevo");
        }
        String respuesta = ejecutor.ejecutar(accion.tipo(), accion.payload());
        almacen.guardar(accion.conEstatus(AccionPendiente.CONFIRMADA));
        return respuesta;
    }

    /** Devuelve la acción para mostrarla antes de confirmar. Si ya venció, la marca como EXPIRADA. */
    public AccionPendiente consultar(UUID id, String usuario) {
        AccionPendiente accion = propia(id, usuario);
        if (AccionPendiente.PENDIENTE.equals(accion.estatus()) && reloj.instant().isAfter(accion.expiraUtc())) {
            AccionPendiente expirada = accion.conEstatus(AccionPendiente.EXPIRADA);
            almacen.guardar(expirada);
            return expirada;
        }
        return accion;
    }

    /** El usuario descarta la acción sin ejecutarla. */
    public void cancelar(UUID id, String usuario) {
        AccionPendiente accion = propia(id, usuario);
        if (!AccionPendiente.PENDIENTE.equals(accion.estatus())) {
            throw new AccionNoPendienteException("La acción ya está " + accion.estatus().toLowerCase());
        }
        almacen.guardar(accion.conEstatus(AccionPendiente.CANCELADA));
    }

    private AccionPendiente propia(UUID id, String usuario) {
        return almacen.buscar(id)
                .filter(a -> a.usuario().equals(usuario))
                .orElseThrow(() -> new AccionNoEncontradaException("Acción no encontrada"));
    }
}
