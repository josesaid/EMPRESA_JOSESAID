package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.mx.josesaid.develop.nexus.application.AuditoriaConsultas;
import com.mx.josesaid.develop.nexus.application.RegistroConsulta;

/** Guarda la auditoría de consultas al asistente con JPA. */
@Component
public class AuditoriaJpa implements AuditoriaConsultas {

    private final ConsultaRepositorio repositorio;

    /** Recibe el repositorio de consultas. */
    public AuditoriaJpa(ConsultaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /** Registra una consulta en la auditoría. */
    @Override
    public void registrar(RegistroConsulta r) {
        repositorio.save(new ConsultaEntidad(
                UUID.randomUUID(), r.usuario(), r.preguntaRedactada(), r.respuestaResumen(),
                r.herramientas(), r.tokensEntrada(), r.tokensSalida(), r.creadoUtc()));
    }
}
