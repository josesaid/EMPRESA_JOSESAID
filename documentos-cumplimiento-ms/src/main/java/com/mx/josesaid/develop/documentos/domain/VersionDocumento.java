package com.mx.josesaid.develop.documentos.domain;

import java.time.Instant;
import java.util.UUID;

/** Versión inmutable de un documento. La referencia apunta al archivo cifrado; nunca se expone en la API. */
public record VersionDocumento(UUID id, int numero, String hashSha256, String almacenamientoRef, Instant creadoUtc) {
}
