package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;

/** Posible duplicado encontrado: criterio ("correo", "nombre" o "razon_social"), tipo de registro e identificador. */
public record Coincidencia(String criterio, String tipoRegistro, UUID id) {
}
