package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.application.Comandos.NuevoConsentimiento;
import com.mx.josesaid.develop.clientes.domain.Consentimiento;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ConsentimientoRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ContactoRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConsentimientoService {

    private final ContactoRepository contactos;
    private final ConsentimientoRepository consentimientos;
    private final RegistradorEventos eventos;
    private final Clock reloj;

    public ConsentimientoService(ContactoRepository contactos, ConsentimientoRepository consentimientos,
                                 RegistradorEventos eventos, Clock reloj) {
        this.contactos = contactos;
        this.consentimientos = consentimientos;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public Consentimiento otorgar(UUID contactoId, NuevoConsentimiento cmd) {
        if (!contactos.existsById(contactoId)) {
            throw new EntidadNoEncontradaException("contacto", contactoId);
        }
        return consentimientos.save(Consentimiento.otorgado(UUID.randomUUID(), contactoId, cmd.finalidad(),
                cmd.fechaOtorgado(), cmd.medio(), cmd.vigencia(), Instant.now(reloj)));
    }

    /** Idempotente: el evento ConsentimientoRevocado se emite solo la primera vez. */
    public Consentimiento revocar(UUID consentimientoId) {
        Consentimiento consentimiento = consentimientos.findById(consentimientoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("consentimiento", consentimientoId));
        if (consentimiento.revocar(Instant.now(reloj))) {
            eventos.registrar("consentimiento", consentimientoId, "ConsentimientoRevocado", Map.of(
                    "consentimientoId", consentimientoId.toString(),
                    "contactoId", consentimiento.getContactoId().toString(),
                    "finalidad", consentimiento.getFinalidad()));
        }
        return consentimiento;
    }
}
