package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.mx.josesaid.develop.nexus.application.ConsumoTokens;

@Component
public class ConsumoTokensJpa implements ConsumoTokens {

    private final ConsultaRepositorio repositorio;

    public ConsumoTokensJpa(ConsultaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public long desde(String usuario, Instant desde) {
        return repositorio.sumarTokens(usuario, desde);
    }

    @Override
    public long delServicio(Instant desde) {
        return repositorio.sumarTokensServicio(desde);
    }
}
