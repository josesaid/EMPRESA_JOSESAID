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
    public static final String TIPO_COTIZACION_ENVIAR = "COTIZACION_ENVIAR";
    public static final String TIPO_COTIZACION_ACEPTAR = "COTIZACION_ACEPTAR";
    public static final String TIPO_COTIZACION_A_PEDIDO = "COTIZACION_A_PEDIDO";
    public static final String TIPO_COTIZACION_CREAR = "COTIZACION_CREAR";
    public static final String TIPO_PEDIDO_REEMPLAZAR_LINEAS = "PEDIDO_REEMPLAZAR_LINEAS";
    public static final String TIPO_PEDIDO_DEVOLVER = "PEDIDO_DEVOLVER";
    public static final String TIPO_PROSPECTO_ESTATUS = "PROSPECTO_ESTATUS";
    public static final String TIPO_PROSPECTO_CONVERTIR = "PROSPECTO_CONVERTIR";
    public static final String TIPO_INTERACCION_REGISTRAR = "INTERACCION_REGISTRAR";
    public static final String TIPO_POLIZA_REGISTRAR = "POLIZA_REGISTRAR";
    public static final String TIPO_DOCUMENTO_ELIMINAR = "DOCUMENTO_ELIMINAR";
    public static final String TIPO_CLIENTE_CREAR = "CLIENTE_CREAR";
    public static final String TIPO_CLIENTE_ACTUALIZAR = "CLIENTE_ACTUALIZAR";
    public static final String TIPO_CLIENTE_CONTACTO_AGREGAR = "CLIENTE_CONTACTO_AGREGAR";
    public static final String TIPO_PROVEEDOR_CREAR = "PROVEEDOR_CREAR";
    public static final String TIPO_ORDEN_COMPRA_CREAR = "ORDEN_COMPRA_CREAR";
    public static final String TIPO_ORDEN_COMPRA_RECEPCION = "ORDEN_COMPRA_RECEPCION";
    public static final String TIPO_ORDEN_COMPRA_CANCELAR = "ORDEN_COMPRA_CANCELAR";
    public static final String TIPO_ARTICULO_CREAR = "ARTICULO_CREAR";
    public static final String TIPO_MOVIMIENTO_REGISTRAR = "MOVIMIENTO_REGISTRAR";
    public static final String TIPO_ORDEN_OPERACION_CREAR = "ORDEN_OPERACION_CREAR";
    public static final String TIPO_COMPROBANTE_PAGO_REGISTRAR = "COMPROBANTE_PAGO_REGISTRAR";
    public static final String TIPO_CLIENTE_ESTATUS = "CLIENTE_ESTATUS";
    public static final String TIPO_ALMACEN_CREAR = "ALMACEN_CREAR";
    public static final String TIPO_PEDIDO_ENTREGAR = "PEDIDO_ENTREGAR";
    public static final String TIPO_INDICADOR_REGISTRAR = "INDICADOR_REGISTRAR";
    /** El alta no la ejecuta Nexus: la pantalla de altas crea el usuario con la contraseña que escribe el administrador. */
    public static final String TIPO_USUARIO_ALTA = "USUARIO_ALTA";
    public static final Duration VIGENCIA = Duration.ofMinutes(10);

    private final AlmacenAcciones almacen;
    private final EjecutorAcciones ejecutor;
    private final Clock reloj;

    /** Recibe el almacén donde se guardan las acciones, el ejecutor que las aplica y el reloj para calcular vencimientos. */
    public AccionesPendientes(AlmacenAcciones almacen, EjecutorAcciones ejecutor, Clock reloj) {
        this.almacen = almacen;
        this.ejecutor = ejecutor;
        this.reloj = reloj;
    }

    /** Guarda una acción nueva en estado PENDIENTE. Vence a los 10 minutos (VIGENCIA). Nada se ejecuta todavía. */
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
        if (TIPO_USUARIO_ALTA.equals(accion.tipo())) {
            throw new AccionNoPendienteException("Las altas de usuario se completan desde la pantalla de altas");
        }
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

    /** Marca un alta como completada. La creación del usuario ocurre en usuarios-accesos, no aquí. */
    public void completarAlta(UUID id, String usuario) {
        AccionPendiente accion = altaVigente(id, usuario);
        almacen.guardar(accion.conEstatus(AccionPendiente.CONFIRMADA));
    }

    /**
     * Devuelve un alta pendiente, vigente y del usuario. Si ya venció la marca como expirada.
     * No cambia nada si la acción sigue vigente.
     */
    public AccionPendiente altaVigente(UUID id, String usuario) {
        AccionPendiente accion = propia(id, usuario);
        if (!TIPO_USUARIO_ALTA.equals(accion.tipo())) {
            throw new AccionNoPendienteException("La acción no es un alta de usuario");
        }
        if (!AccionPendiente.PENDIENTE.equals(accion.estatus())) {
            throw new AccionNoPendienteException("La acción ya está " + accion.estatus().toLowerCase());
        }
        if (reloj.instant().isAfter(accion.expiraUtc())) {
            almacen.guardar(accion.conEstatus(AccionPendiente.EXPIRADA));
            throw new AccionNoPendienteException("La acción expiró; pide a Nexus que la prepare de nuevo");
        }
        return accion;
    }

    /** Busca la acción y verifica que pertenezca al usuario indicado. Si no existe o es de otro usuario responde lo mismo, para no revelar que existe. */
    private AccionPendiente propia(UUID id, String usuario) {
        return almacen.buscar(id)
                .filter(a -> a.usuario().equals(usuario))
                .orElseThrow(() -> new AccionNoEncontradaException("Acción no encontrada"));
    }
}
