package com.mx.josesaid.develop.clientes.infrastructure.persistence;

import com.mx.josesaid.develop.clientes.domain.Contacto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoRepository extends JpaRepository<Contacto, UUID> {

    List<Contacto> findByCorreoHash(String correoHash);

    Optional<Contacto> findFirstByClienteIdAndPrincipalTrue(UUID clienteId);
}
