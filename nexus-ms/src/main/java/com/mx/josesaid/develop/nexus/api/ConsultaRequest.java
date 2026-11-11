package com.mx.josesaid.develop.nexus.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pregunta del usuario al asistente. Debe tener texto y no pasar de 2000 caracteres. */
public record ConsultaRequest(
        @NotBlank(message = "La pregunta es obligatoria")
        @Size(max = 2000, message = "La pregunta no puede superar 2000 caracteres")
        String pregunta) {
}
