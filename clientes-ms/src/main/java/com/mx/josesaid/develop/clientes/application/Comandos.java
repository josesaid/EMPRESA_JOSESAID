package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.domain.Fuente;
import com.mx.josesaid.develop.clientes.domain.MedioConsentimiento;
import com.mx.josesaid.develop.clientes.domain.RolContacto;
import com.mx.josesaid.develop.clientes.domain.TipoInteraccion;
import java.time.Instant;
import java.time.LocalDate;

/** Comandos de entrada de los casos de uso. La capa application no conoce DTOs HTTP. */
public final class Comandos {

    /** Clase de utilidad con los comandos de la capa de aplicación; no se instancia. */
    private Comandos() {
    }

    /** Datos de un contacto recibidos para registrar. */
    public record DatosContacto(String nombre, String correo, RolContacto rol) {
    }

    /** Datos para registrar un prospecto. Si confirmarDuplicado es verdadero, se acepta una coincidencia parcial. */
    public record NuevoProspecto(String nombre, String correo, String rfc, Fuente fuente, boolean confirmarDuplicado) {
    }

    /** Datos para convertir un prospecto en cliente. */
    public record ConvertirProspecto(String razonSocial, String rfc, DatosContacto contactoPrincipal) {
    }

    /** Datos para dar de alta un cliente directo. */
    public record NuevoCliente(String razonSocial, String rfc, DatosContacto contactoPrincipal, boolean confirmarDuplicado) {
    }

    /** Datos de un contacto a agregar a un cliente. */
    public record NuevoContacto(String nombre, String correo, RolContacto rol, boolean principal) {
    }

    /** Datos de una interacción comercial. */
    public record NuevaInteraccion(TipoInteraccion tipo, Instant fecha, String resumen) {
    }

    /** Datos de un consentimiento a registrar. */
    public record NuevoConsentimiento(String finalidad, Instant fechaOtorgado, MedioConsentimiento medio, LocalDate vigencia) {
    }
}
