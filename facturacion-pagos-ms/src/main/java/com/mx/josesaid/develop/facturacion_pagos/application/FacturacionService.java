package com.mx.josesaid.develop.facturacion_pagos.application;

import com.mx.josesaid.develop.facturacion_pagos.domain.Comprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.ComprobanteRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de facturación. Cada cambio de estado publica un evento en el outbox
 * dentro de la misma transacción.
 */
@Service
public class FacturacionService {

    public static final String EVENTO_COMPROBANTE_EMITIDO = "comprobante-emitido";
    public static final String EVENTO_COMPROBANTE_CANCELADO = "comprobante-cancelado";
    public static final String EVENTO_PAGO_REGISTRADO = "pago-registrado";

    private final ComprobanteRepositorio repositorio;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public FacturacionService(ComprobanteRepositorio repositorio, PublicadorEventos eventos, Clock reloj) {
        this.repositorio = repositorio;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional
    public Comprobante crearBorrador(UUID ordenId, String moneda, BigDecimal subtotal, BigDecimal tasaIva) {
        return repositorio.guardar(Comprobante.crearBorrador(ordenId, moneda, subtotal, tasaIva));
    }

    @Transactional
    public Comprobante emitir(UUID id) {
        Comprobante comprobante = obtener(id);
        comprobante.emitir();
        Comprobante guardado = repositorio.guardar(comprobante);
        eventos.publicar(new EventoDominio(EVENTO_COMPROBANTE_EMITIDO, id.toString(), Map.of(
                "comprobanteId", id.toString(),
                "folio", guardado.getFolio(),
                "total", guardado.getTotal().toPlainString())));
        return guardado;
    }

    @Transactional
    public Comprobante cancelar(UUID id, String motivo) {
        Comprobante comprobante = obtener(id);
        comprobante.cancelar(motivo);
        Comprobante guardado = repositorio.guardar(comprobante);
        eventos.publicar(new EventoDominio(EVENTO_COMPROBANTE_CANCELADO, id.toString(), Map.of(
                "comprobanteId", id.toString(),
                "motivo", guardado.getMotivoCancelacion())));
        return guardado;
    }

    /** Registra un pago. Si no trae fecha, se usa la fecha actual del servidor. */
    @Transactional
    public Comprobante registrarPago(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {
        Comprobante comprobante = obtener(id);
        LocalDate fecha = fechaPago != null ? fechaPago : LocalDate.now(reloj);
        var pago = comprobante.registrarPago(monto, fecha, referencia);
        Comprobante guardado = repositorio.guardar(comprobante);
        eventos.publicar(new EventoDominio(EVENTO_PAGO_REGISTRADO, id.toString(), Map.of(
                "comprobanteId", id.toString(),
                "monto", pago.getMonto().toPlainString(),
                "saldo", guardado.saldo().toPlainString())));
        return guardado;
    }

    @Transactional(readOnly = true)
    public Comprobante buscarPorFolio(String folio) {
        return repositorio.buscarPorFolio(folio.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante no encontrado con folio: " + folio));
    }

    @Transactional(readOnly = true)
    public Comprobante obtener(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comprobante no encontrado: " + id));
    }
}
