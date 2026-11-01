package com.mx.josesaid.develop.nexus.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.ContextoContable.CuentaCatalogo;
import com.mx.josesaid.develop.nexus.application.PolizaSugerida.LineaSugerida;

/**
 * Sugiere una póliza y la deja PENDIENTE. Nunca la registra: el registro ocurre solo cuando
 * una persona confirma la acción con su propia sesión (EjecutorAcciones).
 */
public final class ServicioPolizas {

    static final Set<String> TIPOS = Set.of("INGRESO", "EGRESO", "DIARIO");

    private final ContextoContable contexto;
    private final SugeridorPolizas sugeridor;
    private final AccionesPendientes acciones;
    private final PresupuestoTokens presupuesto;
    private final Clock reloj;
    private final ObjectMapper json = new ObjectMapper();

    public ServicioPolizas(ContextoContable contexto, SugeridorPolizas sugeridor, AccionesPendientes acciones,
                           PresupuestoTokens presupuesto, Clock reloj) {
        this.contexto = contexto;
        this.sugeridor = sugeridor;
        this.acciones = acciones;
        this.presupuesto = presupuesto;
        this.reloj = reloj;
    }

    public AccionPendiente preparar(String usuario, String descripcion) {
        presupuesto.exigirDisponible(usuario);
        List<CuentaCatalogo> catalogo = contexto.catalogo();
        PolizaSugerida sugerencia = sugeridor.sugerir(usuario, descripcion, catalogo, contexto.ejemplosRecientes());

        Map<String, CuentaCatalogo> porCodigo = catalogo.stream()
                .collect(Collectors.toMap(CuentaCatalogo::codigo, Function.identity(), (a, b) -> a));
        validar(sugerencia, porCodigo);

        LocalDate fecha = LocalDate.now(reloj);
        String payload = payload(sugerencia, porCodigo, fecha);
        return acciones.preparar(usuario, AccionesPendientes.TIPO_POLIZA_REGISTRAR, payload,
                resumen(sugerencia, fecha));
    }

    static void validar(PolizaSugerida s, Map<String, CuentaCatalogo> porCodigo) {
        if (s == null || !TIPOS.contains(s.tipo())) {
            throw new SugerenciaPolizaInvalidaException("Tipo de póliza no válido");
        }
        if (Double.isNaN(s.confianza()) || s.confianza() < 0 || s.confianza() > 1) {
            throw new SugerenciaPolizaInvalidaException("Nivel de confianza fuera de rango");
        }
        if (s.lineas() == null || s.lineas().size() < 2) {
            throw new SugerenciaPolizaInvalidaException("Una póliza necesita al menos dos movimientos");
        }
        BigDecimal cargos = BigDecimal.ZERO;
        BigDecimal abonos = BigDecimal.ZERO;
        for (LineaSugerida l : s.lineas()) {
            if (!porCodigo.containsKey(l.codigoCuenta())) {
                throw new SugerenciaPolizaInvalidaException("La cuenta " + l.codigoCuenta() + " no está en el catálogo activo");
            }
            if (l.cargo() == null || l.abono() == null || l.cargo().signum() < 0 || l.abono().signum() < 0) {
                throw new SugerenciaPolizaInvalidaException("Montos inválidos en la cuenta " + l.codigoCuenta());
            }
            if ((l.cargo().signum() > 0) == (l.abono().signum() > 0)) {
                throw new SugerenciaPolizaInvalidaException("Cada movimiento debe tener solo cargo o solo abono");
            }
            cargos = cargos.add(l.cargo());
            abonos = abonos.add(l.abono());
        }
        if (cargos.signum() == 0 || cargos.compareTo(abonos) != 0) {
            throw new SugerenciaPolizaInvalidaException("La póliza no cuadra: cargos " + cargos + ", abonos " + abonos);
        }
    }

    private String payload(PolizaSugerida s, Map<String, CuentaCatalogo> porCodigo, LocalDate fecha) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        for (LineaSugerida l : s.lineas()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cuentaId", porCodigo.get(l.codigoCuenta()).id().toString());
            m.put("cargo", l.cargo());
            m.put("abono", l.abono());
            movimientos.add(m);
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipo", s.tipo());
        cuerpo.put("fecha", fecha.toString());
        cuerpo.put("movimientos", movimientos);
        try {
            return json.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la póliza sugerida", e);
        }
    }

    private static String resumen(PolizaSugerida s, LocalDate fecha) {
        BigDecimal total = s.lineas().stream().map(LineaSugerida::cargo).reduce(BigDecimal.ZERO, BigDecimal::add);
        String cuentas = s.lineas().stream()
                .map(l -> (l.cargo().signum() > 0 ? "cargo " : "abono ") + l.codigoCuenta()
                        + " " + (l.cargo().signum() > 0 ? l.cargo() : l.abono()))
                .collect(Collectors.joining("; "));
        String texto = "Registrar póliza " + s.tipo() + " del " + fecha + " por " + total
                + " (confianza " + Math.round(s.confianza() * 100) + "%): " + cuentas
                + ". " + (s.motivo() == null ? "" : s.motivo());
        return texto.length() > 900 ? texto.substring(0, 900) : texto;
    }
}
