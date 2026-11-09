package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Herramientas sobre clientes-ms, por gateway-ms con el token del usuario.
 * Las de lectura consultan datos; las de escritura solo preparan una acción que el usuario confirma en la aplicación.
 * La respuesta se redacta antes de volver al modelo.
 */
public final class HerramientasClientes {

    static final String RUTA_BASE = "/clientes-ms/v1/clientes";
    static final String RUTA_PROSPECTOS = "/clientes-ms/v1/prospectos";
    static final String RUTA_METRICAS = "/clientes-ms/v1/metricas/conversion";
    static final String NO_ENCONTRADO_CLIENTE = "No encontré ese cliente.";
    private static final String NO_DISPONIBLE = "La preparación de acciones no está disponible en este momento.";
    private static final Pattern RFC = Pattern.compile("[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}");
    private static final Pattern MES = Pattern.compile("\\d{4}-\\d{2}");
    private static final Set<String> ROLES_CONTACTO = Set.of("COMPRAS", "PAGOS", "ASISTENCIA");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient gateway;
    private final AccionesPendientes acciones;

    public HerramientasClientes(RestClient gateway) {
        this(gateway, null);
    }

    public HerramientasClientes(RestClient gateway, AccionesPendientes acciones) {
        this.gateway = gateway;
        this.acciones = acciones;
    }

    @Tool(name = "buscar_clientes",
          description = "Busca clientes por RFC o por nombre. Devuelve hasta 10 resultados con su última interacción.")
    public String buscarClientes(
            @ToolParam(description = "RFC completo y exacto (no admite búsqueda parcial). Vacío si se busca por nombre.", required = false) String rfc,
            @ToolParam(description = "Nombre o razón social. Vacío si se busca por RFC.", required = false) String nombre) {
        RegistroHerramientasUsadas.registrar("buscar_clientes");
        String cuerpo = gateway.get()
                .uri(constructor -> {
                    var uri = constructor.path(RUTA_BASE).queryParam("size", 10);
                    if (rfc != null && !rfc.isBlank()) {
                        uri.queryParam("rfc", rfc.trim());
                    }
                    if (nombre != null && !nombre.isBlank()) {
                        uri.queryParam("nombre", nombre.trim());
                    }
                    return uri.build();
                })
                .retrieve()
                .body(String.class);
        return redactar(cuerpo);
    }

    @Tool(name = "obtener_cliente",
          description = "Devuelve el detalle de un cliente por su identificador (UUID).")
    public String obtenerCliente(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String id) {
        RegistroHerramientasUsadas.registrar("obtener_cliente");
        String cuerpo = gateway.get()
                .uri(RUTA_BASE + "/{id}", id)
                .retrieve()
                .body(String.class);
        return redactar(cuerpo);
    }

    @Tool(name = "consultar_interacciones_cliente",
          description = "Lista las interacciones registradas de un cliente: tipo, fecha y resumen.")
    public String consultarInteraccionesCliente(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String clienteId) {
        RegistroHerramientasUsadas.registrar("consultar_interacciones_cliente");
        UUID id = identificador(clienteId);
        if (id == null) {
            return "El identificador del cliente no es válido.";
        }
        return lectura(RUTA_BASE + "/" + id + "/interacciones", NO_ENCONTRADO_CLIENTE);
    }

    @Tool(name = "obtener_prospecto",
          description = "Devuelve un prospecto por su identificador UUID: nombre, fuente, estatus y fecha de alta.")
    public String obtenerProspecto(
            @ToolParam(description = "Identificador UUID del prospecto", required = true) String prospectoId) {
        RegistroHerramientasUsadas.registrar("obtener_prospecto");
        UUID id = identificador(prospectoId);
        if (id == null) {
            return "El identificador del prospecto no es válido.";
        }
        return lectura(RUTA_PROSPECTOS + "/" + id, "No encontré ese prospecto.");
    }

    @Tool(name = "consultar_metricas_conversion",
          description = "Devuelve la conversión de prospectos a clientes de un mes. El mes es AAAA-MM.")
    public String consultarMetricasConversion(
            @ToolParam(description = "Mes en formato AAAA-MM, por ejemplo 2026-09", required = true) String mes) {
        RegistroHerramientasUsadas.registrar("consultar_metricas_conversion");
        String valor = mes == null ? "" : mes.trim();
        if (!MES.matcher(valor).matches()) {
            return "El mes debe tener formato AAAA-MM, por ejemplo 2026-09.";
        }
        String cuerpo = gateway.get()
                .uri(constructor -> constructor.path(RUTA_METRICAS).queryParam("mes", valor).build())
                .retrieve()
                .body(String.class);
        return redactar(cuerpo);
    }

    @Tool(name = "crear_cliente",
          description = "Prepara el alta de un cliente con su contacto principal. NO lo crea: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Solo ADMIN puede usarla.")
    public String crearCliente(
            @ToolParam(description = "Razón social del cliente", required = true) String razonSocial,
            @ToolParam(description = "RFC del cliente, en mayúsculas", required = true) String rfc,
            @ToolParam(description = "Nombre del contacto principal", required = true) String contactoNombre,
            @ToolParam(description = "Correo electrónico del contacto principal", required = true) String contactoCorreo,
            @ToolParam(description = "Rol del contacto: COMPRAS, PAGOS o ASISTENCIA", required = true) String contactoRol) {
        RegistroHerramientasUsadas.registrar("crear_cliente");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        if (vacio(razonSocial) || razonSocial.trim().length() > 250) {
            return "La razón social es obligatoria y de hasta 250 caracteres.";
        }
        String rfcNormalizado = rfc == null ? "" : rfc.trim().toUpperCase();
        if (!RFC.matcher(rfcNormalizado).matches()) {
            return "El RFC no tiene un formato válido.";
        }
        String rol = normalizar(contactoRol);
        if (!ROLES_CONTACTO.contains(rol)) {
            return "El rol del contacto debe ser COMPRAS, PAGOS o ASISTENCIA.";
        }
        if (!correoValido(contactoCorreo)) {
            return "El correo del contacto no es válido.";
        }
        if (vacio(contactoNombre) || contactoNombre.trim().length() > 200) {
            return "El nombre del contacto es obligatorio y de hasta 200 caracteres.";
        }
        Map<String, Object> contacto = new LinkedHashMap<>();
        contacto.put("nombre", contactoNombre.trim());
        contacto.put("correo", contactoCorreo.trim());
        contacto.put("rol", rol);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("razonSocial", razonSocial.trim());
        cuerpo.put("rfc", rfcNormalizado);
        cuerpo.put("contactoPrincipal", contacto);
        String resumen = "Alta de cliente " + razonSocial.trim() + " (RFC " + rfcNormalizado + ")";
        return guardarAccion(AccionesPendientes.TIPO_CLIENTE_CREAR, cuerpo, resumen, "Alta preparada, no creada.");
    }

    @Tool(name = "actualizar_cliente",
          description = "Prepara el cambio de razón social de un cliente. NO lo cambia: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Solo ADMIN puede usarla.")
    public String actualizarCliente(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String clienteId,
            @ToolParam(description = "Nueva razón social", required = true) String razonSocial) {
        RegistroHerramientasUsadas.registrar("actualizar_cliente");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(clienteId);
        if (id == null) {
            return "El identificador del cliente no es válido.";
        }
        if (vacio(razonSocial) || razonSocial.trim().length() > 250) {
            return "La razón social es obligatoria y de hasta 250 caracteres.";
        }
        if (!existeCliente(id)) {
            return NO_ENCONTRADO_CLIENTE;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("clienteId", id.toString());
        cuerpo.put("razonSocial", razonSocial.trim());
        String resumen = "Cambiar la razón social del cliente " + id + " a " + razonSocial.trim();
        return guardarAccion(AccionesPendientes.TIPO_CLIENTE_ACTUALIZAR, cuerpo, resumen, "Cambio preparado, no aplicado.");
    }

    @Tool(name = "agregar_contacto_cliente",
          description = "Prepara el alta de un contacto en un cliente existente. NO lo agrega: devuelve un resumen y un accion_id. "
                  + "El usuario debe confirmarlo en la aplicación. Solo ADMIN puede usarla.")
    public String agregarContactoCliente(
            @ToolParam(description = "Identificador UUID del cliente", required = true) String clienteId,
            @ToolParam(description = "Nombre del contacto", required = true) String nombre,
            @ToolParam(description = "Correo electrónico del contacto", required = true) String correo,
            @ToolParam(description = "Rol del contacto: COMPRAS, PAGOS o ASISTENCIA", required = true) String rol,
            @ToolParam(description = "true si será el contacto principal; vacío o false en otro caso", required = false) String principal) {
        RegistroHerramientasUsadas.registrar("agregar_contacto_cliente");
        if (acciones == null) {
            return NO_DISPONIBLE;
        }
        UUID id = identificador(clienteId);
        if (id == null) {
            return "El identificador del cliente no es válido.";
        }
        if (vacio(nombre) || nombre.trim().length() > 200) {
            return "El nombre del contacto es obligatorio y de hasta 200 caracteres.";
        }
        if (!correoValido(correo)) {
            return "El correo del contacto no es válido.";
        }
        String rolNormalizado = normalizar(rol);
        if (!ROLES_CONTACTO.contains(rolNormalizado)) {
            return "El rol del contacto debe ser COMPRAS, PAGOS o ASISTENCIA.";
        }
        if (!existeCliente(id)) {
            return NO_ENCONTRADO_CLIENTE;
        }
        boolean esPrincipal = "true".equalsIgnoreCase(principal == null ? "" : principal.trim());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("clienteId", id.toString());
        cuerpo.put("nombre", nombre.trim());
        cuerpo.put("correo", correo.trim());
        cuerpo.put("rol", rolNormalizado);
        cuerpo.put("principal", esPrincipal);
        String resumen = "Agregar contacto " + nombre.trim() + " (" + rolNormalizado + ") al cliente " + id
                + (esPrincipal ? ", como principal" : "");
        return guardarAccion(AccionesPendientes.TIPO_CLIENTE_CONTACTO_AGREGAR, cuerpo, resumen,
                "Alta de contacto preparada, no agregada.");
    }

    private String guardarAccion(String tipo, Map<String, Object> cuerpo, String resumen, String encabezado) {
        AccionPendiente accion = acciones.preparar(usuarioActual(), tipo, aJson(cuerpo), resumen);
        return redactar(encabezado + " " + resumen + ". accion_id: " + accion.id()
                + ". Vence en 10 minutos; el usuario debe confirmarlo en la aplicación.");
    }

    private boolean existeCliente(UUID id) {
        try {
            gateway.get().uri(RUTA_BASE + "/" + id).retrieve().body(String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    private String lectura(String ruta, String noEncontrado) {
        try {
            String cuerpo = gateway.get().uri(ruta).retrieve().body(String.class);
            return redactar(cuerpo);
        } catch (HttpClientErrorException.NotFound e) {
            return noEncontrado;
        }
    }

    private static String redactar(String texto) {
        return FiltroDatosSensibles.redactar(texto, EnumSet.of(FiltroDatosSensibles.Tipo.RFC)).texto();
    }

    private static UUID identificador(String valor) {
        try {
            return valor == null ? null : UUID.fromString(valor.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static boolean correoValido(String correo) {
        return !vacio(correo) && correo.trim().length() <= 254 && correo.contains("@");
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private static String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static String aJson(Map<String, Object> datos) {
        try {
            return JSON.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la acción", e);
        }
    }
}
