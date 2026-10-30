package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.application.Comandos.ConvertirProspecto;
import com.mx.josesaid.develop.clientes.application.Comandos.NuevoProspecto;
import com.mx.josesaid.develop.clientes.domain.Cliente;
import com.mx.josesaid.develop.clientes.domain.Contacto;
import com.mx.josesaid.develop.clientes.domain.EstatusProspecto;
import com.mx.josesaid.develop.clientes.domain.Prospecto;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ClienteRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ContactoRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ProspectoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProspectoService {

    private final ProspectoRepository prospectos;
    private final ClienteRepository clientes;
    private final ContactoRepository contactos;
    private final DetectorDuplicados duplicados;
    private final RegistradorEventos eventos;
    private final MeterRegistry metricas;
    private final Clock reloj;

    public ProspectoService(ProspectoRepository prospectos, ClienteRepository clientes, ContactoRepository contactos,
                            DetectorDuplicados duplicados, RegistradorEventos eventos,
                            MeterRegistry metricas, Clock reloj) {
        this.prospectos = prospectos;
        this.clientes = clientes;
        this.contactos = contactos;
        this.duplicados = duplicados;
        this.eventos = eventos;
        this.metricas = metricas;
        this.reloj = reloj;
    }

    public Prospecto crear(NuevoProspecto cmd) {
        String rfc = Normalizacion.rfc(cmd.rfc());
        duplicados.validarRfcUnico(rfc);
        duplicados.validarPosibles(cmd.correo(), cmd.nombre(), cmd.confirmarDuplicado());

        Prospecto guardado = prospectos.save(
                Prospecto.nuevo(UUID.randomUUID(), cmd.nombre(), cmd.correo(), rfc, cmd.fuente(),
                        LocalDate.now(reloj), Instant.now(reloj)));
        metricas.counter("clientes.altas", "tipo", "prospecto").increment();
        return guardado;
    }

    @Transactional(readOnly = true)
    public Prospecto obtener(UUID id) {
        return prospectos.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("prospecto", id));
    }

    /** Idempotente: repetir el estatus actual no hace cambios. CONVERTIDO solo se alcanza con convertir(). */
    public Prospecto cambiarEstatus(UUID id, EstatusProspecto destino) {
        Prospecto prospecto = obtener(id);
        prospecto.cambiarEstatus(destino, Instant.now(reloj));
        return prospecto;
    }

    /**
     * Convierte un prospecto CALIFICADO en cliente ACTIVO con contacto principal.
     * Idempotente: si el prospecto ya está CONVERTIDO devuelve el cliente existente.
     */
    public Cliente convertir(UUID prospectoId, ConvertirProspecto cmd) {
        Prospecto prospecto = obtener(prospectoId);
        if (prospecto.getEstatus() == EstatusProspecto.CONVERTIDO) {
            return clientes.findFirstByProspectoId(prospectoId)
                    .orElseThrow(() -> new IllegalStateException("Prospecto convertido sin cliente asociado: " + prospectoId));
        }

        String rfc = Normalizacion.rfc(cmd.rfc());
        duplicados.validarRfcUnico(rfc);

        Instant ahora = Instant.now(reloj);
        UUID clienteId = UUID.randomUUID();
        UUID contactoId = UUID.randomUUID();
        Cliente cliente = clientes.save(
                Cliente.nuevo(clienteId, cmd.razonSocial(), rfc, prospecto.getId(), LocalDate.now(reloj), ahora));
        contactos.save(Contacto.nuevo(contactoId, clienteId, cmd.contactoPrincipal().nombre(),
                cmd.contactoPrincipal().correo(), cmd.contactoPrincipal().rol(), true, ahora));
        prospecto.marcarConvertido(ahora);

        eventos.registrar("cliente", clienteId, "ClienteCreado", Map.of(
                "clienteId", clienteId.toString(),
                "prospectoId", prospectoId.toString(),
                "contactoPrincipalId", contactoId.toString(),
                "estatus", cliente.getEstatus().name()));
        metricas.counter("clientes.conversiones").increment();
        return cliente;
    }
}
