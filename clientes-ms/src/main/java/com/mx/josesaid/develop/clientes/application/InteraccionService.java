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

@Service
@Transactional
public class InteraccionService {

    private final ClienteRepository clientes;
    private final InteraccionRepository interacciones;
    private final Clock reloj;

    public InteraccionService(ClienteRepository clientes, InteraccionRepository interacciones, Clock reloj) {
        this.clientes = clientes;
        this.interacciones = interacciones;
        this.reloj = reloj;
    }

    public Interaccion registrar(UUID clienteId, NuevaInteraccion cmd, UUID responsableId) {
        verificarCliente(clienteId);
        return interacciones.save(Interaccion.nueva(UUID.randomUUID(), clienteId, cmd.tipo(), cmd.fecha(),
                responsableId, cmd.resumen(), Instant.now(reloj)));
    }

    @Transactional(readOnly = true)
    public Page<Interaccion> listar(UUID clienteId, Pageable pageable) {
        verificarCliente(clienteId);
        return interacciones.findByClienteId(clienteId, pageable);
    }

    private void verificarCliente(UUID clienteId) {
        if (!clientes.existsById(clienteId)) {
            throw new EntidadNoEncontradaException("cliente", clienteId);
        }
    }
}
