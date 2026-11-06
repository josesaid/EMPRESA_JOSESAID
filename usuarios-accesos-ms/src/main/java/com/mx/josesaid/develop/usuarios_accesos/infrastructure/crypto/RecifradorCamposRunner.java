package com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Re-cifra correo y nombre de usuarios a la versión activa. Solo corre con
 * {@code USUARIOS_RECIFRADO_ACTIVO=true}; por defecto no toca datos. Registra solo conteos, nunca valores.
 * El índice {@code correo_indice} no cambia, así que no hace falta recalcularlo.
 */
@Component
@ConditionalOnProperty(name = "USUARIOS_RECIFRADO_ACTIVO", havingValue = "true")
public class RecifradorCamposRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecifradorCamposRunner.class);
    private static final int TAMANO_LOTE = 500;
    private static final List<String[]> CAMPOS = List.of(
            new String[] {"usuarios", "correo"},
            new String[] {"usuarios", "nombre_completo"});

    private final RecifradorJdbc almacen;
    private final CampoCifradoConverter convertidor = new CampoCifradoConverter();

    public RecifradorCamposRunner(RecifradorJdbc almacen) {
        this.almacen = almacen;
    }

    @Override
    public void run(ApplicationArguments args) {
        RecifradorPorLotes recifrador = new RecifradorPorLotes(almacen, TAMANO_LOTE,
                valor -> convertidor.convertToDatabaseColumn(convertidor.convertToEntityAttribute(valor)));
        for (String[] campo : CAMPOS) {
            int total = recifrador.ejecutar(campo[0], campo[1]);
            log.info("Campos re-cifrados en {}.{}: {}", campo[0], campo[1], total);
        }
    }
}
