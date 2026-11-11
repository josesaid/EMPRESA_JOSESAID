package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramienta de solo lectura sobre usuarios-accesos-ms, por gateway-ms con el token del usuario.
 * Solo ADMIN puede consultar usuarios. La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasUsuarios {

    static final String RUTA_BASE = "/usuarios-accesos-ms/v1";

    /** Roles que un alta puede asignar. JEFE_AREA no se asigna desde el alta. */
    static final Set<String> ROLES_ALTA = Set.of("ADMIN", "FINANZAS", "CONTADOR", "VENTAS", "COMPRAS",
            "ALMACEN", "OPERACIONES", "CALIDAD", "DIRECCION", "LEGAL");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;
    private final CodigoConfirmacion codigos;

    /** Versión sin registro de acciones ni códigos: solo permite consultar. */
    public HerramientasUsuarios(RestClient gateway) {
        this(gateway, null, null);
    }

    /** Recibe el cliente del gateway, el registro de acciones y el generador de códigos. Las altas se confirman con código. */
    public HerramientasUsuarios(RestClient gateway, AccionesPendientes acciones, CodigoConfirmacion codigos) {
        this.gateway = gateway;
        this.acciones = acciones;
        this.codigos = codigos;
    }

    /** Herramienta del asistente: devuelve los datos de un usuario. No expone contraseñas. */
    @Tool(name = "consultar_usuario",
          description = "Devuelve los datos de un usuario por su identificador UUID: correo y nombre enmascarados, estatus, roles y área.")
    public String consultarUsuario(
            @ToolParam(description = "Identificador UUID del usuario", required = true) String usuarioId) {
        RegistroHerramientasUsadas.registrar("consultar_usuario");
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_BASE + "/usuarios/{id}")
                        .build(UUID.fromString(usuarioId.trim())))
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }

    /**
     * Prepara un alta de usuario. No crea nada ni recibe contraseña: el administrador la escribe en la
     * pantalla de altas, que la envía directo a usuarios-accesos. Nexus solo recibe el código de confirmación.
     */
    @Tool(name = "preparar_alta_usuario",
          description = "Prepara el alta de un usuario nuevo. No crea el usuario y no pide contraseña: "
                  + "devuelve el identificador de la acción y un código de confirmación. "
                  + "El administrador completa el alta en la pantalla de altas, donde escribe la contraseña temporal.")
    public String prepararAltaUsuario(
            @ToolParam(description = "Correo electrónico del usuario nuevo", required = true) String correo,
            @ToolParam(description = "Nombre completo del usuario nuevo", required = true) String nombreCompleto,
            @ToolParam(description = "Roles separados por coma. Valores: ADMIN, FINANZAS, CONTADOR, VENTAS, COMPRAS, "
                    + "ALMACEN, OPERACIONES, CALIDAD, DIRECCION, LEGAL", required = true) String roles) {
        RegistroHerramientasUsadas.registrar("preparar_alta_usuario");
        String correoLimpio = correo == null ? "" : correo.trim();
        String nombreLimpio = nombreCompleto == null ? "" : nombreCompleto.trim();
        List<String> rolesLista = roles == null ? List.of()
                : Arrays.stream(roles.split(",")).map(String::trim).filter(r -> !r.isEmpty()).toList();
        if (!correoLimpio.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "El correo no es válido. Pide el correo completo del usuario.";
        }
        if (nombreLimpio.isEmpty()) {
            return "Falta el nombre completo del usuario.";
        }
        if (rolesLista.isEmpty() || !ROLES_ALTA.containsAll(rolesLista)) {
            return "Roles inválidos. Usa solo: " + String.join(", ", ROLES_ALTA) + ".";
        }
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        String payload = aJson(Map.of("correo", correoLimpio, "nombreCompleto", nombreLimpio, "roles", rolesLista));
        AccionPendiente accion = acciones.preparar(usuario, AccionesPendientes.TIPO_USUARIO_ALTA, payload,
                "Alta de usuario " + correoLimpio + " con roles " + String.join(", ", rolesLista));
        return "Alta preparada: acción " + accion.id() + ", vence " + accion.expiraUtc()
                + ". Código de confirmación: " + codigos.generar(accion.id(), accion.usuario())
                + ". El usuario aún no existe. Completa el alta en /usuarios/altas/" + accion.id()
                + ", donde se escribe la contraseña temporal.";
    }

    /** Convierte un mapa en JSON para guardarlo en la acción pendiente. */
    private static String aJson(Map<String, Object> datos) {
        try {
            return JSON.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el alta", e);
        }
    }

    /** Herramienta del asistente: lista los usuarios de la empresa. */
    @Tool(name = "listar_usuarios",
          description = "Lista los últimos 15 usuarios, del más reciente al más antiguo: correo y nombre enmascarados, "
                  + "estatus, roles y área. Solo para administradores.")
    public String listarUsuarios() {
        RegistroHerramientasUsadas.registrar("listar_usuarios");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/usuarios")
                .retrieve()
                .body(String.class);
        return FiltroDatosSensibles.redactar(cuerpo).texto();
    }
}
