package com.mx.josesaid.develop.ui.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.http.HttpSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Entrega un access token vigente para llamar a las APIs. Renueva la sesión si está por expirar o si el servicio
 * rechaza el token. Las renovaciones de una misma sesión se serializan con un lock en Redis: el refresh token rota
 * en cada uso, así que dos renovaciones con el mismo token revocarían la sesión.
 */
@Service
public class ServicioSesion {

    private static final Duration MARGEN = Duration.ofSeconds(60);
    private static final Duration TTL_LOCK = Duration.ofSeconds(10);
    private static final long ESPERA_LOCK_MS = 5_000;

    private final RestClient gateway;
    private final StringRedisTemplate redis;
    private final SessionRepository<?> sesiones;

    /** Recibe el cliente del gateway, Redis para el lock de renovación y el repositorio de sesiones de Spring Session. */
    public ServicioSesion(RestClient gatewayClient, StringRedisTemplate redis, SessionRepository<?> sesiones) {
        this.gateway = gatewayClient;
        this.redis = redis;
        this.sesiones = sesiones;
    }

    /** Entrega un access token vigente. Renueva la sesión si le quedan menos de 60 segundos de vida. */
    public String accessTokenVigente() {
        SesionTokens sesion = sesionActual();
        if (Instant.now().plus(MARGEN).isAfter(sesion.getExpiraEn())) {
            renovarSesion(sesion);
        }
        return sesion.getAccessToken();
    }

    /**
     * Ejecuta una llamada con el token vigente. Si el servicio responde 401 (token revocado o firmado con una
     * clave anterior), renueva la sesión una sola vez y repite la llamada. Si la renovación falla, la sesión expiró.
     */
    public <T> T ejecutar(Function<String, T> llamada) {
        try {
            return llamada.apply(accessTokenVigente());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 401) {
                throw e;
            }
            SesionTokens sesion = sesionActual();
            renovarSesion(sesion);
            return llamada.apply(sesion.getAccessToken());
        }
    }

    /** Devuelve los tokens guardados en la sesión del usuario actual. Si no hay sesión, lanza SesionExpiradaException. */
    private SesionTokens sesionActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof SesionTokens sesion)) {
            throw new SesionExpiradaException("No hay sesión activa");
        }
        return sesion;
    }

    /** Renueva la sesión del usuario actual. */
    private void renovarSesion(SesionTokens sesion) {
        renovarProtegido(idSesionActual(), sesion);
    }

    /**
     * Renueva bajo lock por sesión. Si otra petición ya renovó el token (el almacén tiene uno distinto), adopta
     * ese en lugar de volver a usar el refresh token, que ya no sirve. Package-private para las pruebas.
     */
    void renovarProtegido(String idSesion, SesionTokens sesion) {
        if (idSesion == null) {
            refrescar(sesion);
            persistir();
            return;
        }
        String clave = "ui:renovacion:" + idSesion;
        boolean conLock = adquirir(clave);
        try {
            SesionTokens almacen = leerDelAlmacen(idSesion);
            if (almacen != null && !almacen.getAccessToken().equals(sesion.getAccessToken())) {
                sesion.actualizar(almacen.getAccessToken(), almacen.getRefreshToken(), almacen.getExpiraEn());
            } else {
                refrescar(sesion);
            }
        } finally {
            if (conLock) {
                redis.delete(clave);
            }
        }
        persistir();
    }

    /** Pide un nuevo access token con el refresh token. El refresh token rota en cada uso. Si el servicio rechaza la petición, la sesión expiró. */
    private void refrescar(SesionTokens sesion) {
        RespuestaLogin nueva;
        try {
            nueva = gateway.post()
                    .uri("/usuarios-accesos-ms/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refreshToken", sesion.getRefreshToken()))
                    .retrieve()
                    .body(RespuestaLogin.class);
        } catch (RestClientException e) {
            throw new SesionExpiradaException("La sesión venció; vuelva a entrar", e);
        }
        if (nueva == null || nueva.accessToken() == null) {
            throw new SesionExpiradaException("La sesión venció; vuelva a entrar");
        }
        sesion.actualizar(nueva.accessToken(), nueva.refreshToken(),
                Instant.now().plusSeconds(nueva.expiresIn()));
    }

    /** Espera hasta obtener el lock. Si no lo consigue a tiempo, sigue sin él (mejor esfuerzo) y devuelve false. */
    private boolean adquirir(String clave) {
        long limite = System.currentTimeMillis() + ESPERA_LOCK_MS;
        while (true) {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(clave, "1", TTL_LOCK))) {
                return true;
            }
            if (System.currentTimeMillis() > limite) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /** Lee los tokens guardados en Redis, no la copia que trajo esta petición al empezar. */
    private SesionTokens leerDelAlmacen(String idSesion) {
        Session guardada = sesiones.findById(idSesion);
        if (guardada == null) {
            return null;
        }
        Object contexto = guardada.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (contexto instanceof SecurityContext sc && sc.getAuthentication() != null
                && sc.getAuthentication().getDetails() instanceof SesionTokens tokens) {
            return tokens;
        }
        return null;
    }

    /** Devuelve el identificador de la sesión HTTP actual, o nulo si no hay una petición web. */
    private static String idSesionActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpSession sesion = atributos.getRequest().getSession(false);
            return sesion == null ? null : sesion.getId();
        }
        return null;
    }

    /**
     * Spring Session no detecta cambios hechos dentro de un objeto ya guardado. Se vuelve a guardar el
     * contexto para que el refresh token rotado llegue a Redis y no se reuse en la siguiente petición.
     */
    private static void persistir() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            atributos.getRequest().getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        }
    }
}
