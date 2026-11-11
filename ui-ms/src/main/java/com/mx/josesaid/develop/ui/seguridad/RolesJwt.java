package com.mx.josesaid.develop.ui.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el claim "roles" del payload del JWT solo para mostrar el menú.
 * La validación de firma y permisos la hace siempre el backend.
 */
public final class RolesJwt {

    private static final Pattern ROLES = Pattern.compile("\"roles\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern TEXTO = Pattern.compile("\"([^\"]+)\"");

    /** Clase de utilidad; no se instancia. */
    private RolesJwt() {
    }

    /** Lee los roles del token JWT sin validar la firma. La validación la hace el gateway; aquí solo se usan para mostrar y ocultar opciones. */
    public static List<String> extraer(String jwt) {
        String[] partes = jwt.split("\\.");
        if (partes.length < 2) {
            return List.of();
        }
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        Matcher m = ROLES.matcher(payload);
        if (!m.find()) {
            return List.of();
        }
        List<String> roles = new java.util.ArrayList<>();
        Matcher t = TEXTO.matcher(m.group(1));
        while (t.find()) {
            roles.add(t.group(1));
        }
        return List.copyOf(roles);
    }
}
