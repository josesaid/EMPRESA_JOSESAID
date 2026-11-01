package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;

/** Puerto de lectura: tokens que un usuario ha consumido desde un instante. La implementación vive en infrastructure. */
public interface ConsumoTokens {

    long desde(String usuario, Instant desde);
}
