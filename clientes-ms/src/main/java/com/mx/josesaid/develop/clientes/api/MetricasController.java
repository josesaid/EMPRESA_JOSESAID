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

@RestController
@RequestMapping("/v1/metricas")
public class MetricasController {

    private final MetricasService servicio;

    public MetricasController(MetricasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/conversion")
    @PreAuthorize("hasRole('VENTAS')")
    public MetricaConversionResponse conversion(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return new MetricaConversionResponse(mes.toString(), servicio.conversionesDelMes(mes));
    }
}
