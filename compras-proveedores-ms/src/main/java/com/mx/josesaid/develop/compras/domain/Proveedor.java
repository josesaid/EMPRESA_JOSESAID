package com.mx.josesaid.develop.compras.domain;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Proveedor. En el dominio los datos fiscales viven en claro; la infraestructura los cifra al guardarlos.
 * El RFC sigue el formato de personas morales (3 letras) o físicas (4 letras), más fecha y homoclave.
 */
public class Proveedor {

    private static final Pattern RFC = Pattern.compile("^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private final String razonSocial;
    private final String rfc;
    private final String correo;
    private final EstatusProveedor estatus;

    private Proveedor(UUID id, String razonSocial, String rfc, String correo, EstatusProveedor estatus) {
        this.id = id;
        this.razonSocial = razonSocial;
        this.rfc = rfc;
        this.correo = correo;
        this.estatus = estatus;
    }

    public static Proveedor crear(String razonSocial, String rfc, String correo) {
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new ReglaDeNegocioException("La razón social es obligatoria");
        }
        String rfcNormalizado = rfc == null ? "" : rfc.trim().toUpperCase(Locale.ROOT);
        if (!RFC.matcher(rfcNormalizado).matches()) {
            throw new ReglaDeNegocioException("El RFC no tiene un formato válido");
        }
        String correoNormalizado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(correoNormalizado).matches()) {
            throw new ReglaDeNegocioException("El correo no tiene un formato válido");
        }
        return new Proveedor(UUID.randomUUID(), razonSocial.trim(), rfcNormalizado, correoNormalizado,
                EstatusProveedor.ACTIVO);
    }

    public static Proveedor reconstruir(UUID id, String razonSocial, String rfc, String correo, EstatusProveedor estatus) {
        return new Proveedor(id, razonSocial, rfc, correo, estatus);
    }

    public UUID getId() { return id; }
    public String getRazonSocial() { return razonSocial; }
    public String getRfc() { return rfc; }
    public String getCorreo() { return correo; }
    public EstatusProveedor getEstatus() { return estatus; }
    public boolean estaActivo() { return estatus == EstatusProveedor.ACTIVO; }
}
