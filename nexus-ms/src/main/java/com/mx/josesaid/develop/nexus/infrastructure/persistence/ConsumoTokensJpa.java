package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.mx.josesaid.develop.nexus.application.ConsumoTokens;

/** Calcula el consumo de tokens a partir de la auditoría. */
@Component
public class ConsumoTokensJpa implements ConsumoTokens {

    private final ConsultaRepositorio repositorio;

    /** Recibe el repositorio de consultas. */
    public ConsumoTokensJpa(ConsultaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /** Suma los tokens consumidos por un usuario desde una fecha. */
    @Override
    public long desde(String usuario, Instant desde) {
        return repositorio.sumarTokens(usuario, desde);
    }

    /** Suma los tokens consumidos por todo el servicio desde una fecha. */
    @Override
    public long delServicio(Instant desde) {
        return repositorio.sumarTokensServicio(desde);
    }
}
