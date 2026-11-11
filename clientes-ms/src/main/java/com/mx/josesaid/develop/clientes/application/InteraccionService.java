package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.application.Comandos.NuevaInteraccion;
import com.mx.josesaid.develop.clientes.domain.Interaccion;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.ClienteRepository;
import com.mx.josesaid.develop.clientes.infrastructure.persistence.InteraccionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra y lista interacciones comerciales de los clientes. */
@Service
@Transactional
public class InteraccionService {

    private final ClienteRepository clientes;
    private final InteraccionRepository interacciones;
    private final Clock reloj;

    /** Recibe los repositorios, el reloj de la aplicación. */
    public InteraccionService(ClienteRepository clientes, InteraccionRepository interacciones, Clock reloj) {
        this.clientes = clientes;
        this.interacciones = interacciones;
        this.reloj = reloj;
    }

    /** Registra una interacción con un cliente. El responsable es el usuario que la registra. Falla si el cliente no existe. */
    public Interaccion registrar(UUID clienteId, NuevaInteraccion cmd, UUID responsableId) {
        verificarCliente(clienteId);
        return interacciones.save(Interaccion.nueva(UUID.randomUUID(), clienteId, cmd.tipo(), cmd.fecha(),
                responsableId, cmd.resumen(), Instant.now(reloj)));
    }

    /** Lista las interacciones de un cliente, paginadas. */
    @Transactional(readOnly = true)
    public Page<Interaccion> listar(UUID clienteId, Pageable pageable) {
        verificarCliente(clienteId);
        return interacciones.findByClienteId(clienteId, pageable);
    }

    /** Verifica que el cliente exista antes de registrar o listar sus interacciones. */
    private void verificarCliente(UUID clienteId) {
        if (!clientes.existsById(clienteId)) {
            throw new EntidadNoEncontradaException("cliente", clienteId);
        }
    }
}
