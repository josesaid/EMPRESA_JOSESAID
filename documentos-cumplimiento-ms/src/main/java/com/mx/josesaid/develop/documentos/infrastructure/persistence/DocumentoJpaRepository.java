package com.mx.josesaid.develop.documentos.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoJpaRepository extends JpaRepository<DocumentoEntity, UUID> {

    @Query("select d from DocumentoEntity d where d.eliminado = false and d.venceEn between :desde and :hasta")
    List<DocumentoEntity> buscarVencenEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("select d from DocumentoEntity d where d.eliminado = false and d.retencionHasta < :hoy "
            + "and d.revisionRetencion = false")
    List<DocumentoEntity> buscarActivosConRetencionVencida(@Param("hoy") LocalDate hoy);
}
