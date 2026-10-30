package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.domain.EstatusCliente;
import com.mx.josesaid.develop.clientes.infrastructure.crypto.IndiceHash;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ClienteRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ContactoRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ProspectoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reglas de duplicados:
 * 1. RFC igual a un cliente no dado de baja: se rechaza siempre (ConflictoRfcException).
 * 2. Correo, nombre o razón social parecidos: se pide confirmación (DuplicadoPosibleException).
 */
@Component
public class DetectorDuplicados {

    static final String METRICA_DUPLICADOS = "clientes.duplicados.detectados";

    private final ClienteRepository clientes;
    private final ProspectoRepository prospectos;
    private final ContactoRepository contactos;
    private final MeterRegistry metricas;

    public DetectorDuplicados(ClienteRepository clientes, ProspectoRepository prospectos,
                              ContactoRepository contactos, MeterRegistry metricas) {
        this.clientes = clientes;
        this.prospectos = prospectos;
        this.contactos = contactos;
        this.metricas = metricas;
    }

    public void validarRfcUnico(String rfc) {
        if (rfc == null) {
            return;
        }
        clientes.findFirstByRfcHashAndEstatusNot(IndiceHash.rfc(rfc), EstatusCliente.BAJA)
                .ifPresent(existente -> {
                    metricas.counter(METRICA_DUPLICADOS, "criterio", "rfc").increment();
                    throw new ConflictoRfcException(existente.getId());
                });
    }

    /** Busca coincidencias parciales por correo y por nombre o razón social. */
    public void validarPosibles(String correo, String nombre, boolean confirmado) {
        List<Coincidencia> coincidencias = new ArrayList<>();

        String correoHash = IndiceHash.correo(correo);
        prospectos.findByCorreoHash(correoHash)
                .forEach(p -> coincidencias.add(new Coincidencia("correo", "prospecto", p.getId())));
        contactos.findByCorreoHash(correoHash)
                .forEach(c -> coincidencias.add(new Coincidencia("correo", "contacto", c.getId())));

        if (nombre != null && !nombre.isBlank()) {
            prospectos.findByNombreHash(IndiceHash.nombre(nombre))
                    .forEach(p -> coincidencias.add(new Coincidencia("nombre", "prospecto", p.getId())));
            clientes.findByRazonSocialIgnoreCase(nombre.trim())
                    .forEach(c -> coincidencias.add(new Coincidencia("razon_social", "cliente", c.getId())));
        }

        if (coincidencias.isEmpty()) {
            return;
        }
        metricas.counter(METRICA_DUPLICADOS, "criterio", "posible").increment();
        if (!confirmado) {
            throw new DuplicadoPosibleException(coincidencias);
        }
    }
}
