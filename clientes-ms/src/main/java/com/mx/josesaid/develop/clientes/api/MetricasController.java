package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.api.ApiDtos.MetricaConversionResponse;
import com.mx.josesaid.develop.clientes.application.MetricasService;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de métricas comerciales. */
@RestController
@RequestMapping("/v1/metricas")
public class MetricasController {

    private final MetricasService servicio;

    /** Recibe el servicio de métricas. */
    public MetricasController(MetricasService servicio) {
        this.servicio = servicio;
    }

    /** Devuelve cuántos prospectos se convirtieron en clientes en el mes indicado. */
    @GetMapping("/conversion")
    @PreAuthorize("hasAnyRole('VENTAS', 'ADMIN')")
    public MetricaConversionResponse conversion(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return new MetricaConversionResponse(mes.toString(), servicio.conversionesDelMes(mes));
    }
}
