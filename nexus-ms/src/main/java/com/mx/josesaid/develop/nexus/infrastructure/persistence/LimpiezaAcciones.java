package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borra las acciones pendientes vencidas hace más de una hora. El nombre y el correo del
 * prospecto viajan en el payload, así que no deben quedar guardados indefinidamente.
 */
@Component
public class LimpiezaAcciones {

    static final Duration RETENCION = Duration.ofHours(1);

    private final AccionPendienteRepositorio repositorio;
    private final Clock reloj;

    /** Recibe el repositorio de acciones pendientes para la limpieza programada. */
    @Autowired
    public LimpiezaAcciones(AccionPendienteRepositorio repositorio) {
        this(repositorio, Clock.systemUTC());
    }

    LimpiezaAcciones(AccionPendienteRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** Borra las acciones que ya no se pueden confirmar y tienen más de un día de vencidas. Se ejecuta de forma programada. */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    @Transactional
    public int borrarVencidas() {
        return repositorio.borrarVencidasAntesDe(reloj.instant().minus(RETENCION));
    }
}
