package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.mx.josesaid.develop.nexus.application.AuditoriaConsultas;
import com.mx.josesaid.develop.nexus.application.RegistroConsulta;

@Component
public class AuditoriaJpa implements AuditoriaConsultas {

    private final ConsultaRepositorio repositorio;

    public AuditoriaJpa(ConsultaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void registrar(RegistroConsulta r) {
        repositorio.save(new ConsultaEntidad(
                UUID.randomUUID(), r.usuario(), r.preguntaRedactada(), r.respuestaResumen(),
                r.herramientas(), r.tokensEntrada(), r.tokensSalida(), r.creadoUtc()));
    }
}
