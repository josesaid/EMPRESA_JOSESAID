package com.mx.josesaid.develop.documentos.infrastructure.persistence;

import com.mx.josesaid.develop.documentos.domain.Documento;
import com.mx.josesaid.develop.documentos.domain.DocumentoRepositorio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implementa el puerto de documentos con JPA. */
@Repository
public class DocumentoRepositorioJpa implements DocumentoRepositorio {

    private final DocumentoJpaRepository jpa;

    /** Recibe el repositorio JPA de documentos. */
    public DocumentoRepositorioJpa(DocumentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda un documento con sus versiones y devuelve la versión persistida. */
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

    /** Busca un documento por identificador. */
    @Override
    public Optional<Documento> buscarPorId(UUID id) {
        return jpa.findById(id).map(DocumentoEntity::aDominio);
    }

    /** Lista los documentos cuyo vencimiento cae entre dos fechas. */
    @Override
    public List<Documento> buscarVencenEntre(LocalDate desde, LocalDate hasta) {
        return jpa.buscarVencenEntre(desde, hasta).stream().map(DocumentoEntity::aDominio).toList();
    }

    /** Busca documentos por título, con límite de resultados. */
    @Override
    public List<Documento> buscarPorTitulo(String texto, int limite) {
        String escapado = texto.toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return jpa.buscarPorTitulo("%" + escapado + "%", org.springframework.data.domain.PageRequest.of(0, limite))
                .stream().map(DocumentoEntity::aDominio).toList();
    }

    /** Lista documentos activos cuya retención ya venció, para revisarlos. */
    @Override
    public List<Documento> buscarActivosConRetencionVencida(LocalDate hoy) {
        return jpa.buscarActivosConRetencionVencida(hoy).stream().map(DocumentoEntity::aDominio).toList();
    }
}
