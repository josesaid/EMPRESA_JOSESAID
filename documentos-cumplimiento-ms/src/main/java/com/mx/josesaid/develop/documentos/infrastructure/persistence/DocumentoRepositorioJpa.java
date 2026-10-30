package com.mx.josesaid.develop.documentos.infrastructure.persistence;

import com.mx.josesaid.develop.documentos.domain.Documento;
import com.mx.josesaid.develop.documentos.domain.DocumentoRepositorio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentoRepositorioJpa implements DocumentoRepositorio {

    private final DocumentoJpaRepository jpa;

    public DocumentoRepositorioJpa(DocumentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Documento guardar(Documento documento) {
        DocumentoEntity e = jpa.findById(documento.getId())
                .map(existente -> {
                    existente.actualizar(documento);
                    return existente;
                })
                .orElseGet(() -> DocumentoEntity.nuevo(documento));
        return jpa.save(e).aDominio();
    }

    @Override
    public Optional<Documento> buscarPorId(UUID id) {
        return jpa.findById(id).map(DocumentoEntity::aDominio);
    }

    @Override
    public List<Documento> buscarVencenEntre(LocalDate desde, LocalDate hasta) {
        return jpa.buscarVencenEntre(desde, hasta).stream().map(DocumentoEntity::aDominio).toList();
    }

    @Override
    public List<Documento> buscarActivosConRetencionVencida(LocalDate hoy) {
        return jpa.buscarActivosConRetencionVencida(hoy).stream().map(DocumentoEntity::aDominio).toList();
    }
}
