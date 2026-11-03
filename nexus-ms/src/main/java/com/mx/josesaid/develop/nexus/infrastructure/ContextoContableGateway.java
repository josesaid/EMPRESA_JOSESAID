package com.mx.josesaid.develop.nexus.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.ContextoContable;

/** Lee el catálogo y las pólizas aplicadas por gateway-ms con el token del usuario; contabilidad valida el rol. */
public final class ContextoContableGateway implements ContextoContable {

    static final String RUTA_CUENTAS = "/contabilidad-ms/v1/cuentas";
    static final String RUTA_POLIZAS = "/contabilidad-ms/v1/polizas";
    static final int MESES = 3;
    static final int MAX_EJEMPLOS = 10;
    static final String APLICADA = "APLICADA";

    private final RestClient gateway;
    private final ZoneId zona;

    public ContextoContableGateway(RestClient gateway, ZoneId zona) {
        this.gateway = gateway;
        this.zona = zona;
    }

    @Override
    public List<CuentaCatalogo> catalogo() {
        RespuestaCuenta[] cuentas = gateway.get().uri(RUTA_CUENTAS).retrieve().body(RespuestaCuenta[].class);
        if (cuentas == null) {
            return List.of();
        }
        return java.util.Arrays.stream(cuentas)
                .map(c -> new CuentaCatalogo(c.id(), c.codigo(), c.nombre()))
                .toList();
    }

    @Override
    public List<PolizaEjemplo> ejemplosRecientes() {
        YearMonth actual = YearMonth.now(zona);
        List<PolizaEjemplo> ejemplos = new ArrayList<>();
        for (int i = 0; i < MESES && ejemplos.size() < MAX_EJEMPLOS; i++) {
            String periodo = actual.minusMonths(i).toString();
            RespuestaPoliza[] polizas = gateway.get()
                    .uri(u -> u.path(RUTA_POLIZAS).queryParam("periodo", periodo).build())
                    .retrieve()
                    .body(RespuestaPoliza[].class);
            if (polizas == null) {
                continue;
            }
            for (RespuestaPoliza p : polizas) {
                // Las reversadas no sirven de ejemplo: la póliza original quedó compensada
                if (!APLICADA.equals(p.estatus()) || ejemplos.size() >= MAX_EJEMPLOS) {
                    continue;
                }
                List<LineaEjemplo> lineas = p.movimientos().stream()
                        .map(m -> new LineaEjemplo(m.cuentaId(), m.cargo(), m.abono()))
                        .toList();
                ejemplos.add(new PolizaEjemplo(p.tipo(), p.fecha(), lineas));
            }
        }
        return ejemplos;
    }

    record RespuestaCuenta(UUID id, String codigo, String nombre) {
    }

    record RespuestaPoliza(String tipo, LocalDate fecha, String estatus, List<RespuestaMovimiento> movimientos) {
    }

    record RespuestaMovimiento(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
