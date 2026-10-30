package com.mx.josesaid.develop.clientes.infrastructure.persistence;

import com.mx.josesaid.develop.clientes.domain.EstatusProspecto;
import com.mx.josesaid.develop.clientes.domain.Prospecto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectoRepository extends JpaRepository<Prospecto, UUID> {

    List<Prospecto> findByCorreoHash(String correoHash);

    List<Prospecto> findByNombreHash(String nombreHash);

    long countByEstatusAndFechaConversionGreaterThanEqualAndFechaConversionLessThan(
            EstatusProspecto estatus, Instant desde, Instant hasta);
}
