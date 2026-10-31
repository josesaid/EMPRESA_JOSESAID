package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas de solo lectura sobre facturacion-pagos-ms, por gateway-ms con el token del usuario.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasFinanzas {

    static final String RUTA_BASE = "/facturacion-pagos-ms/v1";

    private final RestClient gateway;

    public HerramientasFinanzas(RestClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = "consultar_comprobante",
          description = "Devuelve un comprobante por su identificador UUID: folio, subtotal, IVA, total, estatus, "
                  + "estatus de cobro, monto pagado, saldo y lista de pagos.")
    public String consultarComprobante(
            @ToolParam(description = "Identificador UUID del comprobante", required = true) String comprobanteId) {
        RegistroHerramientasUsadas.registrar("consultar_comprobante");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/comprobantes/{id}")
                        .build(UUID.fromString(comprobanteId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
