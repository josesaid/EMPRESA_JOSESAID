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

    /**
     * Datos de un contacto recibidos para registrar.
     *
     * @param nombre componente de tipo {@code String}.
     * @param correo componente de tipo {@code String}.
     * @param rol componente de tipo {@code RolContacto}.
     */
    public record DatosContacto(String nombre, String correo, RolContacto rol) {
    }

    /**
     * Datos para registrar un prospecto. Si confirmarDuplicado es verdadero, se acepta una coincidencia parcial.
     *
     * @param nombre componente de tipo {@code String}.
     * @param correo componente de tipo {@code String}.
     * @param rfc componente de tipo {@code String}.
     * @param fuente componente de tipo {@code Fuente}.
     * @param confirmarDuplicado componente de tipo {@code boolean}.
     */
    public record NuevoProspecto(String nombre, String correo, String rfc, Fuente fuente, boolean confirmarDuplicado) {
    }

    /**
     * Datos para convertir un prospecto en cliente.
     *
     * @param razonSocial componente de tipo {@code String}.
     * @param rfc componente de tipo {@code String}.
     * @param contactoPrincipal componente de tipo {@code DatosContacto}.
     */
    public record ConvertirProspecto(String razonSocial, String rfc, DatosContacto contactoPrincipal) {
    }

    /**
     * Datos para dar de alta un cliente directo.
     *
     * @param razonSocial componente de tipo {@code String}.
     * @param rfc componente de tipo {@code String}.
     * @param contactoPrincipal componente de tipo {@code DatosContacto}.
     * @param confirmarDuplicado componente de tipo {@code boolean}.
     */
    public record NuevoCliente(String razonSocial, String rfc, DatosContacto contactoPrincipal, boolean confirmarDuplicado) {
    }

    /**
     * Datos de un contacto a agregar a un cliente.
     *
     * @param nombre componente de tipo {@code String}.
     * @param correo componente de tipo {@code String}.
     * @param rol componente de tipo {@code RolContacto}.
     * @param principal componente de tipo {@code boolean}.
     */
    public record NuevoContacto(String nombre, String correo, RolContacto rol, boolean principal) {
    }

    /**
     * Datos de una interacción comercial.
     *
     * @param tipo componente de tipo {@code TipoInteraccion}.
     * @param fecha componente de tipo {@code Instant}.
     * @param resumen componente de tipo {@code String}.
     */
    public record NuevaInteraccion(TipoInteraccion tipo, Instant fecha, String resumen) {
    }

    /**
     * Datos de un consentimiento a registrar.
     *
     * @param finalidad componente de tipo {@code String}.
     * @param fechaOtorgado componente de tipo {@code Instant}.
     * @param medio componente de tipo {@code MedioConsentimiento}.
     * @param vigencia componente de tipo {@code LocalDate}.
     */
    public record NuevoConsentimiento(String finalidad, Instant fechaOtorgado, MedioConsentimiento medio, LocalDate vigencia) {
    }
}
