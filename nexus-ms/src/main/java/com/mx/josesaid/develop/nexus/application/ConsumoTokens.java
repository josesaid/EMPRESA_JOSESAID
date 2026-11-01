package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;

/** Puerto de lectura: tokens consumidos desde un instante, por usuario o por todo el servicio. La implementación vive en infrastructure. */
public interface ConsumoTokens {

    long desde(String usuario, Instant desde);

    long delServicio(Instant desde);
}
