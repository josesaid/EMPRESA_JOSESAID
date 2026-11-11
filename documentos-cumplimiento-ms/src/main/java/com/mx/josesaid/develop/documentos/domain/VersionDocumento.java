package com.mx.josesaid.develop.documentos.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Versión inmutable de un documento. La referencia apunta al archivo cifrado; nunca se expone en la API.
 *
 * @param id componente de tipo {@code UUID}.
 * @param numero componente de tipo {@code int}.
 * @param hashSha256 componente de tipo {@code String}.
 * @param almacenamientoRef componente de tipo {@code String}.
 * @param creadoUtc componente de tipo {@code Instant}.
 */
public record VersionDocumento(UUID id, int numero, String hashSha256, String almacenamientoRef, Instant creadoUtc) {
}
