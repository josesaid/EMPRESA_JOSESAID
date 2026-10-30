package com.mx.josesaid.develop.usuarios_accesos.config;

import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crea el primer administrador al arrancar, si se configura con variables de entorno.
 *
 * <p>Resuelve el arranque en frío: crear usuarios requiere un token de ADMIN, y sin un
 * administrador previo nadie podría crearlo. Es idempotente: si el correo ya existe, no hace nada.
 * La contraseña nunca se registra en logs.
 */
@Configuration
public class AdministradorInicialConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicialConfig.class);

    @Bean
    ApplicationRunner administradorInicial(UsuarioService servicio, UsuarioRepositorio repositorio,
                                           PropiedadesAdministradorInicial propiedades) {
        return (ApplicationArguments args) -> {
            String correo = propiedades.correo();
            if (correo == null || correo.isBlank()) {
                return; // no configurado: no se crea nada
            }
            String correoNormalizado = Usuario.normalizarCorreo(correo);
            if (repositorio.existePorCorreo(correoNormalizado)) {
                return;
            }
            servicio.crear(correoNormalizado, "Administrador inicial", propiedades.password(), Set.of(Rol.ADMIN));
            LOG.info("Administrador inicial creado");
        };
    }
}
