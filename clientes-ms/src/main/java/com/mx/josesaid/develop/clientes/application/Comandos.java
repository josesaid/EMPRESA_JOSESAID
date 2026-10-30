package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.domain.Fuente;
import com.mx.josesaid.develop.clientes.domain.MedioConsentimiento;
import com.mx.josesaid.develop.clientes.domain.RolContacto;
import com.mx.josesaid.develop.clientes.domain.TipoInteraccion;
import java.time.Instant;
import java.time.LocalDate;

/** Comandos de entrada de los casos de uso. La capa application no conoce DTOs HTTP. */
public final class Comandos {

    private Comandos() {
    }

    public record DatosContacto(String nombre, String correo, RolContacto rol) {
    }

    public record NuevoProspecto(String nombre, String correo, String rfc, Fuente fuente, boolean confirmarDuplicado) {
    }

    public record ConvertirProspecto(String razonSocial, String rfc, DatosContacto contactoPrincipal) {
    }

    public record NuevoCliente(String razonSocial, String rfc, DatosContacto contactoPrincipal, boolean confirmarDuplicado) {
    }

    public record NuevoContacto(String nombre, String correo, RolContacto rol, boolean principal) {
    }

    public record NuevaInteraccion(TipoInteraccion tipo, Instant fecha, String resumen) {
    }

    public record NuevoConsentimiento(String finalidad, Instant fechaOtorgado, MedioConsentimiento medio, LocalDate vigencia) {
    }
}
