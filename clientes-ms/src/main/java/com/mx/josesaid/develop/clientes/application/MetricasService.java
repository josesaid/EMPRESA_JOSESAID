package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.domain.EstatusProspecto;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ProspectoRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricasService {

    private final ProspectoRepository prospectos;
    private final ZoneId zona;

    public MetricasService(ProspectoRepository prospectos, @Value("${app.zona-presentacion}") String zona) {
        this.prospectos = prospectos;
        this.zona = ZoneId.of(zona);
    }

    /** Prospectos convertidos en el mes indicado. El mes se interpreta en la zona de presentación. */
    @Transactional(readOnly = true)
    public long conversionesDelMes(YearMonth mes) {
        Instant desde = mes.atDay(1).atStartOfDay(zona).toInstant();
        Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant();
        return prospectos.countByEstatusAndFechaConversionGreaterThanEqualAndFechaConversionLessThan(
                EstatusProspecto.CONVERTIDO, desde, hasta);
    }
}
