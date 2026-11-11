package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.domain.Cliente;
import com.mx.josesaid.develop.clientes.domain.Consentimiento;
import com.mx.josesaid.develop.clientes.domain.Contacto;
import com.mx.josesaid.develop.clientes.domain.EstatusCliente;
import com.mx.josesaid.develop.clientes.domain.EstatusProspecto;
import com.mx.josesaid.develop.clientes.domain.Fuente;
import com.mx.josesaid.develop.clientes.domain.Interaccion;
import com.mx.josesaid.develop.clientes.domain.MedioConsentimiento;
import com.mx.josesaid.develop.clientes.domain.Prospecto;
import com.mx.josesaid.develop.clientes.domain.RolContacto;
import com.mx.josesaid.develop.clientes.domain.TipoInteraccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DTOs de la API. Las respuestas enmascaran RFC, correo y nombre. */
public final class ApiDtos {

    /** Formato del RFC de persona moral (3 letras) o física (4 letras), fecha AAMMDD y homoclave. */
    public static final String PATRON_RFC = "[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}";

    /** Contenedor de los DTOs de la API. No se instancia. */
    private ApiDtos() {
    }

    // ----- Peticiones -----

    /** Datos para registrar un prospecto: nombre, correo, RFC, fuente y confirmación de duplicado si ya fue revisado. */
    public record NuevoProspectoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Email @Size(max = 254) String correo,
            @Pattern(regexp = PATRON_RFC) String rfc,
            @NotNull Fuente fuente) {
    }

    /** Cambio de estatus de un prospecto. */
    public record CambioEstatusProspectoRequest(@NotNull EstatusProspecto estatus) {
    }

    /** Cambio de estatus de un cliente. */
    public record CambioEstatusClienteRequest(@NotNull EstatusCliente estatus) {
    }

    /** Datos de un contacto: nombre, correo y rol. */
    public record DatosContactoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotNull RolContacto rol) {
    }

    /** Datos para convertir un prospecto en cliente: razón social, RFC y contacto principal. */
    public record ConvertirProspectoRequest(
            @NotBlank @Size(max = 250) String razonSocial,
            @NotBlank @Pattern(regexp = PATRON_RFC) String rfc,
            @NotNull @Valid DatosContactoRequest contactoPrincipal) {
    }

    /** Datos para dar de alta un cliente directo, con su contacto principal. */
    public record NuevoClienteRequest(
            @NotBlank @Size(max = 250) String razonSocial,
            @NotBlank @Pattern(regexp = PATRON_RFC) String rfc,
            @NotNull @Valid DatosContactoRequest contactoPrincipal) {
    }

    /** Cambio de la razón social de un cliente. */
    public record ActualizarClienteRequest(@NotBlank @Size(max = 250) String razonSocial) {
    }

    /** Datos para agregar un contacto a un cliente, con indicador de principal. */
    public record ContactoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotNull RolContacto rol,
            boolean principal) {
    }

    /** Datos de una interacción comercial: tipo, fecha y resumen. */
    public record NuevaInteraccionRequest(
            @NotNull TipoInteraccion tipo,
            @NotNull Instant fecha,
            @NotBlank @Size(max = 2000) String resumen) {
    }

    /** Datos de un consentimiento: finalidad, fecha, medio y vigencia. */
    public record NuevoConsentimientoRequest(
            @NotBlank @Size(max = 100) String finalidad,
            @NotNull Instant fechaOtorgado,
            @NotNull MedioConsentimiento medio,
            LocalDate vigencia) {
    }

    // ----- Respuestas -----

    /** Respuesta de un prospecto. El correo, el nombre y el RFC van enmascarados. */
    public record ProspectoResponse(
            UUID id,
            String nombre,
            String correo,
            String rfc,
            Fuente fuente,
            EstatusProspecto estatus,
            LocalDate fechaRegistro,
            OffsetDateTime fechaConversion) {

        /** Convierte un prospecto en su respuesta, con fechas en la zona de presentación y datos enmascarados. */
        public static ProspectoResponse de(Prospecto p, Presentacion presentacion) {
            return new ProspectoResponse(p.getId(), Mascara.nombre(p.getNombre()), Mascara.correo(p.getCorreo()),
                    Mascara.rfc(p.getRfc()), p.getFuente(), p.getEstatus(), p.getFechaRegistro(),
                    presentacion.fecha(p.getFechaConversion()));
        }
    }

    /** Respuesta de un cliente. */
    public record ClienteResponse(
            UUID id,
            String razonSocial,
            String rfc,
            EstatusCliente estatus,
            UUID prospectoId,
            LocalDate fechaAlta) {

        /** Convierte un cliente en su respuesta. */
        public static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getRazonSocial(), Mascara.rfc(c.getRfc()), c.getEstatus(),
                    c.getProspectoId(), c.getFechaAlta());
        }
    }

    /** Resumen de un cliente con su última interacción. */
    public record ClienteResumenResponse(ClienteResponse cliente, InteraccionResponse ultimaInteraccion) {
    }

    /** Respuesta de un contacto. El correo va enmascarado. */
    public record ContactoResponse(
            UUID id,
            UUID clienteId,
            String nombre,
            String correo,
            RolContacto rol,
            boolean principal) {

        /** Convierte un contacto en su respuesta, con el correo enmascarado. */
        public static ContactoResponse de(Contacto c) {
            return new ContactoResponse(c.getId(), c.getClienteId(), Mascara.nombre(c.getNombre()),
                    Mascara.correo(c.getCorreo()), c.getRol(), c.isPrincipal());
        }
    }

    /** Respuesta de una interacción. */
    public record InteraccionResponse(
            UUID id,
            UUID clienteId,
            TipoInteraccion tipo,
            OffsetDateTime fecha,
            UUID responsableId,
            String resumen) {

        /** Convierte una interacción en su respuesta, con la fecha en la zona de presentación. */
        public static InteraccionResponse de(Interaccion i, Presentacion presentacion) {
            return new InteraccionResponse(i.getId(), i.getClienteId(), i.getTipo(),
                    presentacion.fecha(i.getFecha()), i.getResponsableId(), i.getResumen());
        }
    }

    /** Respuesta de un consentimiento. */
    public record ConsentimientoResponse(
            UUID id,
            UUID contactoId,
            String finalidad,
            OffsetDateTime fechaOtorgado,
            MedioConsentimiento medio,
            LocalDate vigencia,
            OffsetDateTime revocadoEn) {

        /** Convierte un consentimiento en su respuesta, con fechas en la zona de presentación. */
        public static ConsentimientoResponse de(Consentimiento c, Presentacion presentacion) {
            return new ConsentimientoResponse(c.getId(), c.getContactoId(), c.getFinalidad(),
                    presentacion.fecha(c.getFechaOtorgado()), c.getMedio(), c.getVigencia(),
                    presentacion.fecha(c.getRevocadoAt()));
        }
    }

    /** Conversiones de prospectos a clientes por mes. */
    public record MetricaConversionResponse(String mes, long conversiones) {
    }
}
