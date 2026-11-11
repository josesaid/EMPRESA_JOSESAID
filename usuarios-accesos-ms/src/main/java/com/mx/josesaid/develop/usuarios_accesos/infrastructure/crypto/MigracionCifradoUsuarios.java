package com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cifra los usuarios que existían en texto plano antes de la migración V5. Es idempotente: solo toca
 * filas sin {@code correo_indice}. No registra correos ni nombres en el log.
 */
@Component
public class MigracionCifradoUsuarios implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionCifradoUsuarios.class);

    private final JdbcTemplate jdbc;

    /** Recibe JDBC para leer y actualizar la tabla de usuarios. */
    public MigracionCifradoUsuarios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Al arrancar, cifra los usuarios que todavía tienen correo y nombre en claro. Es idempotente y no registra datos personales. */
    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> pendientes = jdbc.queryForList(
                "SELECT id, correo, nombre_completo FROM usuarios WHERE correo_indice IS NULL");
        if (pendientes.isEmpty()) {
            return;
        }
        CampoCifradoConverter cifrador = new CampoCifradoConverter();
        for (Map<String, Object> fila : pendientes) {
            String correo = (String) fila.get("correo");
            String nombre = (String) fila.get("nombre_completo");
            jdbc.update(
                    "UPDATE usuarios SET correo = ?, nombre_completo = ?, correo_indice = ? WHERE id = ?",
                    cifrador.convertToDatabaseColumn(correo),
                    cifrador.convertToDatabaseColumn(nombre),
                    IndiceCorreo.de(correo),
                    fila.get("id"));
        }
        log.info("Usuarios cifrados en esta ejecución: {}", pendientes.size());
    }
}
