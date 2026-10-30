package com.mx.josesaid.develop.pedidos.application;

import java.util.UUID;

/**
 * Confirma en clientes-ms que el cliente existe y está ACTIVO antes de cotizar o vender.
 *
 * <p>Lanza {@code ReglaDeNegocioException} si el cliente no existe o no está activo, y
 * {@code ServicioNoDisponibleException} si clientes-ms no responde.
 */
public interface ValidadorCliente {

    void exigirActivo(UUID clienteId);
}
