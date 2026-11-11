package com.mx.josesaid.develop.documentos.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Versión inmutable de un documento. La referencia apunta al archivo cifrado; nunca se expone en la API.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param numero numero, de tipo {@code int}.
 * @param hashSha256 hash sha256, de tipo {@code String}.
 * @param almacenamientoRef almacenamiento ref, de tipo {@code String}.
 * @param creadoUtc creado utc, de tipo {@code Instant}.
 */
public record VersionDocumento(UUID id, int numero, String hashSha256, String almacenamientoRef, Instant creadoUtc) {
}
