package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramienta de solo lectura sobre usuarios-accesos-ms, por gateway-ms con el token del usuario.
 * Solo ADMIN puede consultar usuarios. La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasUsuarios {

    static final String RUTA_BASE = "/usuarios-accesos-ms/v1";

    private final RestClient gateway;

    public HerramientasUsuarios(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "consultar_usuario",
          description = "Devuelve los datos de un usuario por su identificador UUID: correo y nombre enmascarados, estatus, roles y área.")
    public String consultarUsuario(
            @ToolParam(description = "Identificador UUID del usuario", required = true) String usuarioId) {
        RegistroHerramientasUsadas.registrar("consultar_usuario");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/usuarios/{id}")
                        .build(UUID.fromString(usuarioId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
