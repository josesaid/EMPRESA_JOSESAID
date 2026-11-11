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

    /** Recibe el cliente del gateway y la zona horaria para calcular el mes actual. */
    public ContextoContableGateway(RestClient gateway, ZoneId zona) {
        this.gateway = gateway;
        this.zona = zona;
    }

    /** Trae el catálogo de cuentas del gateway. Si no hay respuesta, devuelve una lista vacía. */
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

    /** Trae hasta cinco pólizas registradas del mes y del mes anterior, como ejemplos de formato para el modelo. */
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

    /**
     * Cuenta tal como la devuelve el gateway.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param codigo código legible que identifica el registro, de tipo {@code String}.
     * @param nombre nombre descriptivo, de tipo {@code String}.
     */
    record RespuestaCuenta(UUID id, String codigo, String nombre) {
    }

    /**
     * Póliza tal como la devuelve el gateway.
     *
     * @param tipo tipo o clasificación del registro, de tipo {@code String}.
     * @param fecha fecha del registro, de tipo {@code LocalDate}.
     * @param estatus estado actual del registro, de tipo {@code String}.
     * @param movimientos movimientos, de tipo {@code List<RespuestaMovimiento>}.
     */
    record RespuestaPoliza(String tipo, LocalDate fecha, String estatus, List<RespuestaMovimiento> movimientos) {
    }

    /**
     * Movimiento de una póliza: cuenta, cargo y abono.
     *
     * @param cuentaId identificador de la cuenta contable, de tipo {@code UUID}.
     * @param cargo importe del cargo, de tipo {@code BigDecimal}.
     * @param abono importe del abono, de tipo {@code BigDecimal}.
     */
    record RespuestaMovimiento(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
