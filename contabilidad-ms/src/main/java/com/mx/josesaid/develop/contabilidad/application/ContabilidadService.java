package com.mx.josesaid.develop.contabilidad.application;

import com.mx.josesaid.develop.contabilidad.domain.Balanza;
import com.mx.josesaid.develop.contabilidad.domain.ConflictoException;
import com.mx.josesaid.develop.contabilidad.domain.CuentaContable;
import com.mx.josesaid.develop.contabilidad.domain.CuentaRepositorio;
import com.mx.josesaid.develop.contabilidad.domain.Movimiento;
import com.mx.josesaid.develop.contabilidad.domain.Naturaleza;
import com.mx.josesaid.develop.contabilidad.domain.Periodo;
import com.mx.josesaid.develop.contabilidad.domain.PeriodoRepositorio;
import com.mx.josesaid.develop.contabilidad.domain.Poliza;
import com.mx.josesaid.develop.contabilidad.domain.PolizaRepositorio;
import com.mx.josesaid.develop.contabilidad.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.contabilidad.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.contabilidad.domain.TipoPoliza;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de contabilidad. Los eventos se guardan en el outbox en la misma transacción. */
@Service
public class ContabilidadService {

    public static final String EVENTO_POLIZA_APLICADA = "poliza-aplicada";
    public static final String EVENTO_PERIODO_CERRADO = "periodo-cerrado";

    private final CuentaRepositorio cuentas;
    private final PeriodoRepositorio periodos;
    private final PolizaRepositorio polizas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public ContabilidadService(CuentaRepositorio cuentas, PeriodoRepositorio periodos, PolizaRepositorio polizas,
                               PublicadorEventos eventos, Clock reloj) {
        this.cuentas = cuentas;
        this.periodos = periodos;
        this.polizas = polizas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional
    public CuentaContable crearCuenta(String codigo, String nombre, Naturaleza naturaleza) {
        if (codigo != null && cuentas.existePorCodigo(codigo.trim())) {
            throw new ConflictoException("Ya existe la cuenta " + codigo.trim());
        }
        return cuentas.guardar(CuentaContable.crear(codigo, nombre, naturaleza));
    }

    @Transactional(readOnly = true)
    public List<CuentaContable> listarCuentasActivas() {
        return cuentas.listarActivas();
    }

    /** Registra una póliza. Primero valida el cuadre (422), luego que el periodo esté abierto (409). */
    @Transactional
    public Poliza registrarPoliza(TipoPoliza tipo, LocalDate fecha, List<Movimiento> movimientos) {
        Poliza poliza = Poliza.aplicar(tipo, fecha, movimientos);
        validarCuentas(movimientos);
        periodoAbierto(poliza.getPeriodo());
        Poliza guardada = polizas.guardar(poliza);
        eventos.publicar(new EventoDominio(EVENTO_POLIZA_APLICADA, guardada.getId().toString(), Map.of(
                "id", guardada.getId().toString(),
                "periodo", guardada.getPeriodo().toString(),
                "total", guardada.total().toPlainString())));
        return guardada;
    }

    /** Reversa una póliza aplicada con fecha de hoy. Las pólizas no se borran ni se editan. */
    @Transactional
    public Poliza reversarPoliza(UUID id) {
        Poliza original = obtenerPoliza(id);
        LocalDate hoy = LocalDate.now(reloj);
        periodoAbierto(YearMonth.from(hoy));
        Poliza reversa = original.reversar(hoy);
        polizas.guardar(original);
        Poliza guardada = polizas.guardar(reversa);
        eventos.publicar(new EventoDominio(EVENTO_POLIZA_APLICADA, guardada.getId().toString(), Map.of(
                "id", guardada.getId().toString(),
                "periodo", guardada.getPeriodo().toString(),
                "total", guardada.total().toPlainString())));
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Poliza> listarPolizas(YearMonth anioMes) {
        return polizas.buscarPorPeriodo(anioMes);
    }

    @Transactional(readOnly = true)
    public Poliza obtenerPoliza(UUID id) {
        return polizas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Póliza no encontrada: " + id));
    }

    @Transactional
    public Periodo cerrarPeriodo(YearMonth anioMes) {
        Periodo periodo = periodos.buscarPorAnioMes(anioMes)
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo no encontrado: " + anioMes));
        periodo.cerrar();
        Periodo guardado = periodos.guardar(periodo);
        eventos.publicar(new EventoDominio(EVENTO_PERIODO_CERRADO, anioMes.toString(), Map.of(
                "anioMes", anioMes.toString())));
        return guardado;
    }

    @Transactional(readOnly = true)
    public Balanza estadosFinancieros(YearMonth anioMes) {
        periodos.buscarPorAnioMes(anioMes)
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo no encontrado: " + anioMes));
        List<Poliza> lista = polizas.buscarPorPeriodo(anioMes);
        Map<UUID, CuentaContable> mapa = new HashMap<>();
        for (Poliza p : lista) {
            for (Movimiento m : p.getMovimientos()) {
                cuentas.buscarPorId(m.getCuentaId()).ifPresent(c -> mapa.put(c.getId(), c));
            }
        }
        return Balanza.calcular(anioMes, lista, mapa);
    }

    /** Devuelve el periodo abierto, o lo crea si es la primera póliza del mes. Un periodo cerrado lanza 409. */
    private Periodo periodoAbierto(YearMonth anioMes) {
        Periodo periodo = periodos.buscarPorAnioMes(anioMes)
                .orElseGet(() -> periodos.guardar(Periodo.abrir(anioMes)));
        if (periodo.estaCerrado()) {
            throw new ConflictoException("El periodo " + anioMes + " está cerrado");
        }
        return periodo;
    }

    private void validarCuentas(List<Movimiento> movimientos) {
        for (UUID cuentaId : movimientos.stream().map(Movimiento::getCuentaId).distinct().toList()) {
            CuentaContable cuenta = cuentas.buscarPorId(cuentaId)
                    .orElseThrow(() -> new ReglaDeNegocioException("La cuenta no existe: " + cuentaId));
            if (!cuenta.isActiva()) {
                throw new ReglaDeNegocioException("La cuenta está inactiva: " + cuenta.getCodigo());
            }
        }
    }
}
