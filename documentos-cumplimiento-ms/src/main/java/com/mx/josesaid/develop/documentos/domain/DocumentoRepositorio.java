package com.mx.josesaid.develop.documentos.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar documentos, sin depender de JPA. */
public interface DocumentoRepositorio {
    Documento guardar(Documento documento);
    Optional<Documento> buscarPorId(UUID id);
    List<Documento> buscarVencenEntre(LocalDate desde, LocalDate hasta);
    List<Documento> buscarActivosConRetencionVencida(LocalDate hoy);

    /** Documentos no eliminados cuyo título contiene el texto (sin distinguir mayúsculas), por título. */
    List<Documento> buscarPorTitulo(String texto, int limite);
}
