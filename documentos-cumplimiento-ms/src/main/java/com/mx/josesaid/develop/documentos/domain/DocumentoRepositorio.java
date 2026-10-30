package com.mx.josesaid.develop.documentos.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepositorio {
    Documento guardar(Documento documento);
    Optional<Documento> buscarPorId(UUID id);
    List<Documento> buscarVencenEntre(LocalDate desde, LocalDate hasta);
    List<Documento> buscarActivosConRetencionVencida(LocalDate hoy);
}
