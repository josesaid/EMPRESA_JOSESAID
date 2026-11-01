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
        AccionPendiente accion = almacen.buscar(id)
                .filter(a -> a.usuario().equals(usuario))
                .orElseThrow(() -> new AccionNoEncontradaException("Acción no encontrada"));
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
}
