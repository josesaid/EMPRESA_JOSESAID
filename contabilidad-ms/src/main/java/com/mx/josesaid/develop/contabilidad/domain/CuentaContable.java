package com.mx.josesaid.develop.contabilidad.domain;

import java.util.UUID;

/** Cuenta del catálogo. No se elimina: se desactiva. Las cuentas con movimientos conservan su historial. */
public class CuentaContable {

    private final UUID id;
    private final String codigo;
    private final String nombre;
    private final Naturaleza naturaleza;
    private boolean activa;

    private CuentaContable(UUID id, String codigo, String nombre, Naturaleza naturaleza, boolean activa) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.naturaleza = naturaleza;
        this.activa = activa;
    }

    public static CuentaContable crear(String codigo, String nombre, Naturaleza naturaleza) {
        if (codigo == null || codigo.isBlank() || codigo.length() > 20) {
            throw new ReglaDeNegocioException("El código de cuenta es obligatorio (máximo 20 caracteres)");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre de la cuenta es obligatorio");
        }
        if (naturaleza == null) {
            throw new ReglaDeNegocioException("La naturaleza de la cuenta es obligatoria");
        }
        return new CuentaContable(UUID.randomUUID(), codigo.trim(), nombre.trim(), naturaleza, true);
    }

    public static CuentaContable reconstruir(UUID id, String codigo, String nombre, Naturaleza naturaleza, boolean activa) {
        return new CuentaContable(id, codigo, nombre, naturaleza, activa);
    }

    public void desactivar() {
        this.activa = false;
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Naturaleza getNaturaleza() { return naturaleza; }
    public boolean isActiva() { return activa; }
}
