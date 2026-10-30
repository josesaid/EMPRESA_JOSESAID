package com.mx.josesaid.develop.clientes.infrastructure.persistence;

import com.mx.josesaid.develop.clientes.domain.Cliente;
import com.mx.josesaid.develop.clientes.domain.EstatusCliente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findFirstByRfcHashAndEstatusNot(String rfcHash, EstatusCliente estatus);

    Optional<Cliente> findFirstByProspectoId(UUID prospectoId);

    Page<Cliente> findByRfcHash(String rfcHash, Pageable pageable);

    Page<Cliente> findByRazonSocialContainingIgnoreCase(String fragmento, Pageable pageable);

    List<Cliente> findByRazonSocialIgnoreCase(String razonSocial);
}
