package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Re-cifra los campos personales de clientes a la versión activa. Solo corre con
 * {@code CRYPTO_RECIFRADO_ACTIVO=true}; por defecto no toca datos. Registra solo conteos, nunca valores.
 */
@Component
@ConditionalOnProperty(name = "app.crypto.recifrado.activo", havingValue = "true")
public class RecifradorCamposRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecifradorCamposRunner.class);
    private static final int TAMANO_LOTE = 500;
    private static final List<String[]> CAMPOS = List.of(
            new String[] {"prospecto", "nombre_cifrado"},
            new String[] {"prospecto", "correo_cifrado"},
            new String[] {"prospecto", "rfc_cifrado"},
            new String[] {"cliente", "rfc_cifrado"},
            new String[] {"contacto", "nombre_cifrado"},
            new String[] {"contacto", "correo_cifrado"},
            new String[] {"interaccion", "resumen_cifrado"});

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
