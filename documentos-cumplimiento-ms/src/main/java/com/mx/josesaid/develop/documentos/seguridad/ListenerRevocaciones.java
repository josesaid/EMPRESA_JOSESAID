package com.mx.josesaid.develop.documentos.seguridad;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Recibe de usuarios-accesos-ms los cierres de sesión y los aplica a la copia local de revocaciones.
 * Los eventos ajenos se ignoran; un mensaje inválido se registra y se descarta.
 * Cada arranque usa un grupo de consumo nuevo y lee el topic desde el inicio: así reconstruye las
 * revocaciones vigentes. Los eventos vencidos los descarta RevocacionSesiones.
 */
@Component
public class ListenerRevocaciones {

    private static final Logger log = LoggerFactory.getLogger(ListenerRevocaciones.class);

    private final RevocacionSesiones revocaciones;
    private final ObjectMapper mapper;

    /** Recibe la copia local de revocaciones y el lector JSON. */
    public ListenerRevocaciones(RevocacionSesiones revocaciones, ObjectMapper mapper) {
        this.revocaciones = revocaciones;
        this.mapper = mapper;
    }

    /** Aplica al instante un evento de cierre de sesión recibido por Kafka. */
    @KafkaListener(topics = "${seguridad.revocaciones.topico:usuarios-accesos.eventos}",
                   groupId = "documentos-cumplimiento-ms-revocaciones-#{T(java.util.UUID).randomUUID()}",
                   properties = "auto.offset.reset=earliest")
    public void recibir(String mensaje) {
        try {
            Sobre sobre = mapper.readValue(mensaje, Sobre.class);
            String tipo = sobre.tipo();
            if (!"sesion-cerrada".equals(tipo) && !"sesiones-usuario-cerradas".equals(tipo)) {
                return;
            }
            Map<String, Object> datos = sobre.datos() == null ? Map.of() : sobre.datos();
            Instant ocurridoEn = Instant.parse(String.valueOf(datos.get("ocurridoEn")));
            if ("sesion-cerrada".equals(tipo)) {
                revocaciones.revocarSesion(String.valueOf(datos.get("sid")), ocurridoEn);
            } else {
                revocaciones.revocarUsuario(String.valueOf(datos.get("usuarioId")), ocurridoEn);
            }
        } catch (RuntimeException e) {
            log.warn("Evento de revocación descartado: {}", e.getClass().getSimpleName());
        }
    }

    /**
     * Sobre que publica usuarios-accesos-ms por el outbox.
     *
     * @param tipo componente de tipo {@code String}.
     * @param agregadoId componente de tipo {@code String}.
     * @param datos componente de tipo {@code Map<String, Object>}.
     */
    public record Sobre(String tipo, String agregadoId, Map<String, Object> datos) {
    }
}
