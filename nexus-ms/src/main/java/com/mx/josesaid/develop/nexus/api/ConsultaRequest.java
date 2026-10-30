package com.mx.josesaid.develop.nexus.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultaRequest(
        @NotBlank(message = "La pregunta es obligatoria")
        @Size(max = 2000, message = "La pregunta no puede superar 2000 caracteres")
        String pregunta) {
}
