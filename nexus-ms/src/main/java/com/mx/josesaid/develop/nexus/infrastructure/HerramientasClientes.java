package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre clientes-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasClientes {

    static final String RUTA_BASE = "/clientes-ms/v1/clientes";

    private final RestClient gateway;

    public HerramientasClientes(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "buscar_clientes",
          description = "Busca clientes por RFC o por nombre. Devuelve hasta 10 resultados con su última interacción.")
    public String buscarClientes(
            @ToolParam(description = "RFC completo o parcial. Vacío si se busca por nombre.", required = false) String rfc,
            @ToolParam(description = "Nombre o razón social. Vacío si se busca por RFC.", required = false) String nombre) {
        RegistroHerramientasUsadas.registrar("buscar_clientes");
        String cuerpo = gateway.get()
                .uri(constructor -> {
                    var uri = constructor.path(RUTA_BASE).queryParam("size", 10);
                    if (rfc != null && !rfc.isBlank()) {
                        uri.queryParam("rfc", rfc.trim());
                    }
                    if (nombre != null && !nombre.isBlank()) {
                        uri.queryParam("nombre", nombre.trim());
                    }
                    return uri.build();
                })
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    @Tool(name = "obtener_cliente",
          description = "Devuelve el detalle de un cliente por su identificador (UUID).")
    public String obtenerCliente(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String id) {
        RegistroHerramientasUsadas.registrar("obtener_cliente");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/{id}", id)
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
