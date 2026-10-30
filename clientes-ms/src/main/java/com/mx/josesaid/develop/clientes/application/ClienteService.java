package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.application.Comandos.NuevoCliente;
import com.mx.josesaid.develop.clientes.application.Comandos.NuevoContacto;
import com.mx.josesaid.develop.clientes.domain.Cliente;
import com.mx.josesaid.develop.clientes.domain.Contacto;
import com.mx.josesaid.develop.clientes.domain.EstatusCliente;
import com.mx.josesaid.develop.clientes.domain.Interaccion;
import com.mx.josesaid.develop.clientes.infrastructure.crypto.IndiceHash;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ClienteRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ContactoRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.InteraccionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clientes;
    private final ContactoRepository contactos;
    private final InteraccionRepository interacciones;
    private final DetectorDuplicados duplicados;
    private final RegistradorEventos eventos;
    private final MeterRegistry metricas;
    private final Clock reloj;

    public ClienteService(ClienteRepository clientes, ContactoRepository contactos, InteraccionRepository interacciones,
                          DetectorDuplicados duplicados, RegistradorEventos eventos,
                          MeterRegistry metricas, Clock reloj) {
        this.clientes = clientes;
        this.contactos = contactos;
        this.interacciones = interacciones;
        this.duplicados = duplicados;
        this.eventos = eventos;
        this.metricas = metricas;
        this.reloj = reloj;
    }

    /** Alta directa de cliente. Siempre crea un contacto principal. */
    public Cliente crear(NuevoCliente cmd) {
        String rfc = Normalizacion.rfc(cmd.rfc());
        duplicados.validarRfcUnico(rfc);
        duplicados.validarPosibles(cmd.contactoPrincipal().correo(), cmd.razonSocial(), cmd.confirmarDuplicado());

        Instant ahora = Instant.now(reloj);
        UUID clienteId = UUID.randomUUID();
        UUID contactoId = UUID.randomUUID();
        Cliente cliente = clientes.save(Cliente.nuevo(clienteId, cmd.razonSocial(), rfc, null, LocalDate.now(reloj), ahora));
        contactos.save(Contacto.nuevo(contactoId, clienteId, cmd.contactoPrincipal().nombre(),
                cmd.contactoPrincipal().correo(), cmd.contactoPrincipal().rol(), true, ahora));

        eventos.registrar("cliente", clienteId, "ClienteCreado", Map.of(
                "clienteId", clienteId.toString(),
                "contactoPrincipalId", contactoId.toString(),
                "estatus", cliente.getEstatus().name()));
        metricas.counter("clientes.altas", "tipo", "cliente").increment();
        return cliente;
    }

    /** Búsqueda por RFC (exacta) o por nombre (parcial, sin distinguir mayúsculas). Requiere al menos uno. */
    @Transactional(readOnly = true)
    public Page<Cliente> buscar(String rfc, String nombre, Pageable pageable) {
        String rfcNormalizado = Normalizacion.rfc(rfc);
        if (rfcNormalizado != null) {
            return clientes.findByRfcHash(IndiceHash.rfc(rfcNormalizado), pageable);
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("Indique al menos un criterio de búsqueda: rfc o nombre");
        }
        return clientes.findByRazonSocialContainingIgnoreCase(nombre.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Cliente obtener(UUID id) {
        return clientes.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("cliente", id));
    }

    @Transactional(readOnly = true)
    public Optional<Interaccion> ultimaInteraccion(UUID clienteId) {
        return interacciones.findFirstByClienteIdOrderByFechaDesc(clienteId);
    }

    public Cliente actualizarRazonSocial(UUID id, String razonSocial) {
        Cliente cliente = obtener(id);
        cliente.actualizarRazonSocial(razonSocial, Instant.now(reloj));
        return cliente;
    }

    /** Cambia el estatus. Idempotente. Emite ClienteEstatusCambiado solo si el estatus cambia. */
    public Cliente cambiarEstatus(UUID id, EstatusCliente destino) {
        Cliente cliente = obtener(id);
        EstatusCliente anterior = cliente.getEstatus();
        cliente.cambiarEstatus(destino, Instant.now(reloj));
        if (anterior != destino) {
            eventos.registrar("cliente", id, "ClienteEstatusCambiado", Map.of(
                    "clienteId", id.toString(),
                    "estatusAnterior", anterior.name(),
                    "estatusNuevo", destino.name()));
        }
        return cliente;
    }

    /** Agrega un contacto. Si es principal, el anterior principal se desmarca en la misma transacción. */
    public Contacto agregarContacto(UUID clienteId, NuevoContacto cmd) {
        obtener(clienteId);
        Instant ahora = Instant.now(reloj);
        if (cmd.principal()) {
            contactos.findFirstByClienteIdAndPrincipalTrue(clienteId).ifPresent(anterior -> {
                anterior.desmarcarPrincipal(ahora);
                contactos.saveAndFlush(anterior);
            });
        }
        return contactos.saveAndFlush(Contacto.nuevo(UUID.randomUUID(), clienteId, cmd.nombre(), cmd.correo(),
                cmd.rol(), cmd.principal(), ahora));
    }
}
