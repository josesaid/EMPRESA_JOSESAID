package com.mx.josesaid.develop.ui.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.mx.josesaid.develop.ui.altas.AltasCliente;

/** Formularios de alta de clientes y proveedores. Las reglas de rol y de datos las aplica cada microservicio. */
@Controller
public class AltasController {

    /** Mismos valores que RolContacto en clientes-ms. */
    static final List<String> ROLES_CONTACTO = List.of("COMPRAS", "PAGOS", "ASISTENCIA");

    private static final String VISTA_CLIENTE = "altas/cliente";
    private static final String VISTA_PROVEEDOR = "altas/proveedor";
    private static final String NO_DISPONIBLE = "El servicio no está disponible. Intenta más tarde.";

    private final AltasCliente altas;

    /** Recibe el servicio de altas. */
    public AltasController(AltasCliente altas) {
        this.altas = altas;
    }

    /** Muestra el formulario de alta de cliente. */
    @GetMapping("/altas/clientes")
    public String formularioCliente(Authentication usuario, Model modelo) {
        preparar(modelo, usuario, "Nuevo cliente", "clientes");
        modelo.addAttribute("roles", ROLES_CONTACTO);
        return VISTA_CLIENTE;
    }

    /** Envía el alta de cliente con sus datos de contacto. Si falla, vuelve al formulario con el mensaje. */
    @PostMapping("/altas/clientes")
    public String crearCliente(@RequestParam String razonSocial, @RequestParam String rfc,
                               @RequestParam String contactoNombre, @RequestParam String contactoCorreo,
                               @RequestParam String contactoRol, Authentication usuario, Model modelo) {
        preparar(modelo, usuario, "Nuevo cliente", "clientes");
        modelo.addAttribute("roles", ROLES_CONTACTO);
        Map<String, Object> contacto = new LinkedHashMap<>();
        contacto.put("nombre", contactoNombre.trim());
        contacto.put("correo", contactoCorreo.trim());
        contacto.put("rol", contactoRol);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("razonSocial", razonSocial.trim());
        cuerpo.put("rfc", rfc.trim().toUpperCase());
        cuerpo.put("contactoPrincipal", contacto);
        try {
            altas.crearCliente(cuerpo);
            modelo.addAttribute("mensaje", "Cliente creado. Lo puedes consultar en el listado de clientes.");
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeApi(e.getStatusCode().value(), "clientes"));
            modelo.addAttribute("valores", cuerpoParaFormulario(cuerpo, contacto));
        } catch (RestClientException e) {
            modelo.addAttribute("error", NO_DISPONIBLE);
            modelo.addAttribute("valores", cuerpoParaFormulario(cuerpo, contacto));
        }
        return VISTA_CLIENTE;
    }

    /** Muestra el formulario de alta de proveedor. */
    @GetMapping("/altas/proveedores")
    public String formularioProveedor(Authentication usuario, Model modelo) {
        preparar(modelo, usuario, "Nuevo proveedor", "proveedores");
        return VISTA_PROVEEDOR;
    }

    /** Envía el alta de proveedor. Si falla, vuelve al formulario con el mensaje. */
    @PostMapping("/altas/proveedores")
    public String crearProveedor(@RequestParam String razonSocial, @RequestParam String rfc,
                                 @RequestParam String correo, Authentication usuario, Model modelo) {
        preparar(modelo, usuario, "Nuevo proveedor", "proveedores");
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("razonSocial", razonSocial.trim());
        cuerpo.put("rfc", rfc.trim().toUpperCase());
        cuerpo.put("correo", correo.trim());
        try {
            altas.crearProveedor(cuerpo);
            modelo.addAttribute("mensaje", "Proveedor creado. Lo puedes consultar en el listado de proveedores.");
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeApi(e.getStatusCode().value(), "proveedores"));
            modelo.addAttribute("valores", cuerpo);
        } catch (RestClientException e) {
            modelo.addAttribute("error", NO_DISPONIBLE);
            modelo.addAttribute("valores", cuerpo);
        }
        return VISTA_PROVEEDOR;
    }

    /** Agrega al modelo el título, la sección y el usuario de la pantalla de altas. */
    private static void preparar(Model modelo, Authentication usuario, String titulo, String seccion) {
        modelo.addAttribute("titulo", titulo);
        modelo.addAttribute("seccion", seccion);
        modelo.addAttribute("correo", usuario.getName());
    }

    /** Rearma el cuerpo del formulario con los datos de contacto, para volver a mostrarlo si hubo error. */
    private static Map<String, Object> cuerpoParaFormulario(Map<String, Object> cuerpo, Map<String, Object> contacto) {
        Map<String, Object> valores = new LinkedHashMap<>(cuerpo);
        valores.put("contactoNombre", contacto.get("nombre"));
        valores.put("contactoCorreo", contacto.get("correo"));
        valores.put("contactoRol", contacto.get("rol"));
        return valores;
    }

    /** Traduce el estado de la API en un mensaje de alta, según la entidad. */
    private static String mensajeApi(int estado, String entidad) {
        return switch (estado) {
            case 403 -> "Tu rol no puede dar de alta " + entidad + ".";
            case 409 -> "Ya existe un registro con ese RFC o correo. No se creó nada.";
            case 400, 422 -> "Revisa los datos capturados: el RFC y el correo deben tener formato válido.";
            default -> "No se pudo guardar. Revisa los datos e intenta de nuevo.";
        };
    }
}
