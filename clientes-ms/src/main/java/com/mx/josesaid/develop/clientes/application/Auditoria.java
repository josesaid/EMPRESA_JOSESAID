package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registro de accesos a datos personales. Solo registra operación, identificador del recurso y
 * sub del actor. Nunca registra el contenido de los datos.
 */
@Component
public class Auditoria {

    private static final Logger LOG = LoggerFactory.getLogger("auditoria.datos_personales");

    public void registrar(String operacion, UUID recurso, UUID actor) {
        LOG.info("operacion={} recurso={} actor={}", operacion, recurso, actor);
    }
}
