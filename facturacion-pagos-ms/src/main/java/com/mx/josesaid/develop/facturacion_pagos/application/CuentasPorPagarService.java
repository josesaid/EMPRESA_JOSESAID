package com.mx.josesaid.develop.facturacion_pagos.application;

import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagar;
import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagarRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoProveedor;
import com.mx.josesaid.develop.facturacion_pagos.domain.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cuentas por pagar a proveedores. Se alimentan de las órdenes aprobadas en compras-proveedores-ms. */
@Service
public class CuentasPorPagarService {

    public static final String EVENTO_PAGO_PROVEEDOR_REGISTRADO = "pago-proveedor-registrado";

    private final CuentaPorPagarRepositorio cuentas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public CuentasPorPagarService(CuentaPorPagarRepositorio cuentas, PublicadorEventos eventos, Clock reloj) {
        this.cuentas = cuentas;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Crea la cuenta de una orden aprobada. Si ya existe para esa orden, no hace nada: el evento puede llegar dos veces. */
    @Transactional
    public void registrarCuenta(UUID ordenId, UUID proveedorId, String moneda, BigDecimal total) {
        if (cuentas.buscarPorOrdenId(ordenId).isPresent()) {
            return;
        }
        cuentas.guardar(CuentaPorPagar.crear(ordenId, proveedorId, moneda, total));
    }

    @Transactional
    public CuentaPorPagar registrarPago(UUID cuentaId, BigDecimal monto, LocalDate fechaPago, String referencia) {
        CuentaPorPagar cuenta = obtener(cuentaId);
        LocalDate fecha = fechaPago != null ? fechaPago : LocalDate.now(reloj);
        PagoProveedor pago = cuenta.registrarPago(monto, fecha, referencia);
        CuentaPorPagar guardada = cuentas.guardar(cuenta);
        eventos.publicar(new EventoDominio(EVENTO_PAGO_PROVEEDOR_REGISTRADO, cuentaId.toString(), Map.of(
                "cuentaId", cuentaId.toString(),
                "ordenId", guardada.getOrdenId().toString(),
                "proveedorId", guardada.getProveedorId().toString(),
                "moneda", guardada.getMoneda(),
                "monto", pago.getMonto().toPlainString(),
                "saldo", guardada.saldo().toPlainString())));
        return guardada;
    }

    @Transactional(readOnly = true)
    public CuentaPorPagar obtener(UUID id) {
        return cuentas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta por pagar no encontrada: " + id));
    }
}
