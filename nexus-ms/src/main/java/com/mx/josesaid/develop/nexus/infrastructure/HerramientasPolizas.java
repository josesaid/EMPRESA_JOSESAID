package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;
import com.mx.josesaid.develop.nexus.application.ServicioPolizas;

/** Propone una póliza por MCP. Nunca la registra: devuelve el código que se necesita para confirmarla. */
public final class HerramientasPolizas {

    private final ServicioPolizas servicio;
    private final CodigoConfirmacion codigos;

    public HerramientasPolizas(ServicioPolizas servicio, CodigoConfirmacion codigos) {
        this.servicio = servicio;
        this.codigos = codigos;
    }

    @Tool(name = "sugerir_poliza",
          description = "Propone una póliza contable a partir de la descripción de una operación. No la registra. "
                  + "Devuelve el identificador de la acción y un código de confirmación de una sola vez; "
                  + "la póliza solo se registra cuando se confirma con confirmar_accion usando ese código.")
    public String sugerirPoliza(
            @ToolParam(description = "Descripción de la operación contable, con montos y moneda", required = true) String descripcion) {
        RegistroHerramientasUsadas.registrar("sugerir_poliza");
        AccionPendiente accion = servicio.preparar(usuarioActual(), descripcion);
        return "Acción " + accion.id() + " preparada, vence " + accion.expiraUtc() + ". "
                + accion.resumen() + ". Código de confirmación: " + codigos.generar(accion.id(), accion.usuario())
                + ". Nada se ha registrado todavía.";
    }

    private static String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
