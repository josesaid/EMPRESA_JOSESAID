package com.mx.josesaid.develop.usuarios_accesos.application;

import java.util.Map;

import com.mx.josesaid.develop.usuarios_accesos.domain.BloqueoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.CredencialesInvalidasException;
import com.mx.josesaid.develop.usuarios_accesos.domain.RefreshToken;
import com.mx.josesaid.develop.usuarios_accesos.domain.RefreshTokenRepositorio;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import com.mx.josesaid.develop.usuarios_accesos.infrastructure.security.EmisorTokens;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sesiones: emite access token y refresh token, rota el refresh en cada uso y revoca la sesión
 * si se reutiliza un token ya rotado (posible robo).
 */
@Service
public class SesionService {

    /** Vigencia del refresh token desde su último uso. */
    public static final Duration DURACION_REFRESCO = Duration.ofDays(7);

    public static final String EVENTO_SESION_CERRADA = "sesion-cerrada";
    public static final String EVENTO_SESIONES_USUARIO_CERRADAS = "sesiones-usuario-cerradas";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Access token y refresh token en texto plano. Solo se devuelven al cliente, nunca se guardan. */
    public record Par(String accessToken, String refreshToken) {
    }

    private final RefreshTokenRepositorio refrescos;
    private final UsuarioRepositorio usuarios;
    private final EmisorTokens emisor;
    private final Clock reloj;
    private final PublicadorEventos publicador;

    public SesionService(RefreshTokenRepositorio refrescos, UsuarioRepositorio usuarios,
                         EmisorTokens emisor, Clock reloj,
                         PublicadorEventos publicador) {
        this.refrescos = refrescos;
        this.usuarios = usuarios;
        this.emisor = emisor;
        this.reloj = reloj;
        this.publicador = publicador;
    }

    /** Inicia una sesión nueva tras un login exitoso. */
    @Transactional
    public Par iniciar(Usuario usuario) {
        return emitirPar(usuario, UUID.randomUUID(), Instant.now(reloj));
    }

    /**
     * Rota el refresh token. Si el token ya fue usado, revoca toda su familia; esa revocación
     * se conserva aunque la excepción haga rollback, por eso noRollbackFor.
     */
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, BloqueoException.class})
    public Par refrescar(String refrescoPlano) {
        Instant ahora = Instant.now(reloj);
        RefreshToken actual = refrescos.buscarPorHash(hash(refrescoPlano))
                .orElseThrow(SesionService::invalido);
        if (actual.usadoUtc() != null) {
            refrescos.revocarFamilia(actual.familiaId(), ahora);
            throw invalido();
        }
        if (!actual.activo(ahora)) {
            throw invalido();
        }
        Usuario usuario = usuarios.buscarPorId(actual.usuarioId()).orElseThrow(SesionService::invalido);
        usuario.verificarAcceso(ahora);
        usuarios.guardar(usuario);
        refrescos.guardar(actual.comoUsado(ahora));
        return emitirPar(usuario, actual.familiaId(), ahora);
    }

    /** Cierra la sesión del refresh token dado. Si el token no existe, no hace nada. */
    @Transactional
    public void cerrar(String refrescoPlano) {
        refrescos.buscarPorHash(hash(refrescoPlano))
                .ifPresent(t -> {
                    Instant ahora = Instant.now(reloj);
                    refrescos.revocarFamilia(t.familiaId(), ahora);
                    publicador.publicar(new EventoDominio(EVENTO_SESION_CERRADA, t.usuarioId().toString(),
                            Map.of("sid", t.familiaId().toString(), "usuarioId", t.usuarioId().toString(),
                                   "ocurridoEn", ahora.toString())));
                });
    }

    /** Cierra todas las sesiones del usuario. */
    @Transactional
    public void cerrarTodas(UUID usuarioId) {
        Instant ahora = Instant.now(reloj);
        refrescos.revocarTodasDeUsuario(usuarioId, ahora);
        publicador.publicar(new EventoDominio(EVENTO_SESIONES_USUARIO_CERRADAS, usuarioId.toString(),
                Map.of("usuarioId", usuarioId.toString(), "ocurridoEn", ahora.toString())));
    }

    private Par emitirPar(Usuario usuario, UUID familia, Instant ahora) {
        String refresco = nuevoRefresco();
        refrescos.guardar(new RefreshToken(UUID.randomUUID(), usuario.getId(), familia, hash(refresco),
                ahora.plus(DURACION_REFRESCO), null, null));
        return new Par(emisor.emitir(usuario, familia), refresco);
    }

    private static String nuevoRefresco() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static CredencialesInvalidasException invalido() {
        return new CredencialesInvalidasException();
    }
}
