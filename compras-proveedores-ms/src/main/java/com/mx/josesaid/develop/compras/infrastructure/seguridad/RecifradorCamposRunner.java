package com.mx.josesaid.develop.compras.infrastructure.seguridad;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Re-cifra los datos sensibles de proveedores a la versión activa. Solo corre con
 * {@code COMPRAS_RECIFRADO_ACTIVO=true}; por defecto no toca datos. Registra solo conteos, nunca valores.
 * El índice {@code rfc_indice} no cambia, así que no hace falta recalcularlo.
 */
@Component
@ConditionalOnProperty(name = "COMPRAS_RECIFRADO_ACTIVO", havingValue = "true")
public class RecifradorCamposRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecifradorCamposRunner.class);
    private static final int TAMANO_LOTE = 500;
    /** Columnas sensibles que se re-cifran a la versión activa de la clave. */
    private static final List<String[]> CAMPOS = List.of(
            new String[] {"proveedores", "rfc_cifrado"},
            new String[] {"proveedores", "correo_cifrado"});

    private final RecifradorJdbc almacen;
    private final ProtectorDatos protector;

    /** Recibe el acceso JDBC a las columnas y el protector de datos. */
    public RecifradorCamposRunner(RecifradorJdbc almacen, ProtectorDatos protector) {
        this.almacen = almacen;
        this.protector = protector;
    }

    /** Al arrancar, re-cifra las columnas de la lista a la versión activa. */
    @Override
    public void run(ApplicationArguments args) {
        RecifradorPorLotes recifrador = new RecifradorPorLotes(almacen, TAMANO_LOTE,
                valor -> protector.cifrar(protector.descifrar(valor)));
        for (String[] campo : CAMPOS) {
            int total = recifrador.ejecutar(campo[0], campo[1]);
            log.info("Campos re-cifrados en {}.{}: {}", campo[0], campo[1], total);
        }
    }
}
