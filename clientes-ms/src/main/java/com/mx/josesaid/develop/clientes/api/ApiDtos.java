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

    private ApiDtos() {
    }

    // ----- Peticiones -----

    public record NuevoProspectoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Email @Size(max = 254) String correo,
            @Pattern(regexp = PATRON_RFC) String rfc,
            @NotNull Fuente fuente) {
    }

    public record CambioEstatusProspectoRequest(@NotNull EstatusProspecto estatus) {
    }

    public record CambioEstatusClienteRequest(@NotNull EstatusCliente estatus) {
    }

    public record DatosContactoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotNull RolContacto rol) {
    }

    public record ConvertirProspectoRequest(
            @NotBlank @Size(max = 250) String razonSocial,
            @NotBlank @Pattern(regexp = PATRON_RFC) String rfc,
            @NotNull @Valid DatosContactoRequest contactoPrincipal) {
    }

    public record NuevoClienteRequest(
            @NotBlank @Size(max = 250) String razonSocial,
            @NotBlank @Pattern(regexp = PATRON_RFC) String rfc,
            @NotNull @Valid DatosContactoRequest contactoPrincipal) {
    }

    public record ActualizarClienteRequest(@NotBlank @Size(max = 250) String razonSocial) {
    }

    public record ContactoRequest(
            @NotBlank @Size(max = 200) String nombre,
            @NotBlank @Email @Size(max = 254) String correo,
            @NotNull RolContacto rol,
            boolean principal) {
    }

    public record NuevaInteraccionRequest(
            @NotNull TipoInteraccion tipo,
            @NotNull Instant fecha,
            @NotBlank @Size(max = 2000) String resumen) {
    }

    public record NuevoConsentimientoRequest(
            @NotBlank @Size(max = 100) String finalidad,
            @NotNull Instant fechaOtorgado,
            @NotNull MedioConsentimiento medio,
            LocalDate vigencia) {
    }

    // ----- Respuestas -----

    public record ProspectoResponse(
            UUID id,
            String nombre,
            String correo,
            String rfc,
            Fuente fuente,
            EstatusProspecto estatus,
            LocalDate fechaRegistro,
            OffsetDateTime fechaConversion) {

        public static ProspectoResponse de(Prospecto p, Presentacion presentacion) {
            return new ProspectoResponse(p.getId(), Mascara.nombre(p.getNombre()), Mascara.correo(p.getCorreo()),
                    Mascara.rfc(p.getRfc()), p.getFuente(), p.getEstatus(), p.getFechaRegistro(),
                    presentacion.fecha(p.getFechaConversion()));
        }
    }

    public record ClienteResponse(
            UUID id,
            String razonSocial,
            String rfc,
            EstatusCliente estatus,
            UUID prospectoId,
            LocalDate fechaAlta) {

        public static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getRazonSocial(), Mascara.rfc(c.getRfc()), c.getEstatus(),
                    c.getProspectoId(), c.getFechaAlta());
        }
    }

    public record ClienteResumenResponse(ClienteResponse cliente, InteraccionResponse ultimaInteraccion) {
    }

    public record ContactoResponse(
            UUID id,
            UUID clienteId,
            String nombre,
            String correo,
            RolContacto rol,
            boolean principal) {

        public static ContactoResponse de(Contacto c) {
            return new ContactoResponse(c.getId(), c.getClienteId(), Mascara.nombre(c.getNombre()),
                    Mascara.correo(c.getCorreo()), c.getRol(), c.isPrincipal());
        }
    }

    public record InteraccionResponse(
            UUID id,
            UUID clienteId,
            TipoInteraccion tipo,
            OffsetDateTime fecha,
            UUID responsableId,
            String resumen) {

        public static InteraccionResponse de(Interaccion i, Presentacion presentacion) {
            return new InteraccionResponse(i.getId(), i.getClienteId(), i.getTipo(),
                    presentacion.fecha(i.getFecha()), i.getResponsableId(), i.getResumen());
        }
    }

    public record ConsentimientoResponse(
            UUID id,
            UUID contactoId,
            String finalidad,
            OffsetDateTime fechaOtorgado,
            MedioConsentimiento medio,
            LocalDate vigencia,
            OffsetDateTime revocadoEn) {

        public static ConsentimientoResponse de(Consentimiento c, Presentacion presentacion) {
            return new ConsentimientoResponse(c.getId(), c.getContactoId(), c.getFinalidad(),
                    presentacion.fecha(c.getFechaOtorgado()), c.getMedio(), c.getVigencia(),
                    presentacion.fecha(c.getRevocadoAt()));
        }
    }

    public record MetricaConversionResponse(String mes, long conversiones) {
    }
}
