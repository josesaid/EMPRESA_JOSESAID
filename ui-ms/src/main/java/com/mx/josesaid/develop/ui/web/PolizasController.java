package com.mx.josesaid.develop.ui.web;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.mx.josesaid.develop.ui.contabilidad.ContabilidadCliente;
import com.mx.josesaid.develop.ui.contabilidad.CuentaDto;
import com.mx.josesaid.develop.ui.contabilidad.PolizaDto;

/** Pantallas de pólizas contables. La reversa solo la puede pedir un contador. */
@Controller
@RequestMapping("/polizas")
public class PolizasController {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final ContabilidadCliente contabilidad;

    /** Recibe el servicio de contabilidad. */
    public PolizasController(ContabilidadCliente contabilidad) {
        this.contabilidad = contabilidad;
    }

    /** Lista las pólizas del periodo indicado, o del mes actual. */
    @GetMapping
    public String listado(@RequestParam(required = false) String periodo, Authentication usuario, Model modelo) {
        String periodoElegido = periodoValido(periodo);
        modelo.addAttribute("correo", usuario.getName());
        modelo.addAttribute("titulo", "Pólizas");
        modelo.addAttribute("periodo", periodoElegido);
        try {
            modelo.addAttribute("polizas", contabilidad.listarPolizas(periodoElegido));
        } catch (HttpClientErrorException.Forbidden e) {
            modelo.addAttribute("polizas", List.of());
            modelo.addAttribute("error", "Tu rol no tiene acceso a las pólizas contables.");
        } catch (RestClientException e) {
            modelo.addAttribute("polizas", List.of());
            modelo.addAttribute("error", "No se pudieron consultar las pólizas. Intenta más tarde.");
        }
        return "polizas/listado";
    }

    /** Muestra el detalle de una póliza con sus movimientos y el nombre de cada cuenta. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable UUID id, Authentication usuario, Model modelo,
                          @RequestParam(required = false) Boolean revertida,
                          @RequestParam(required = false) String errorReversa) {
        modelo.addAttribute("correo", usuario.getName());
        modelo.addAttribute("titulo", "Detalle de póliza");
        modelo.addAttribute("puedeRevertir", esContador(usuario));
        if (Boolean.TRUE.equals(revertida)) {
            modelo.addAttribute("mensaje", "Póliza revertida. Se registró su reversa.");
        }
        if (errorReversa != null) {
            modelo.addAttribute("errorReversa", errorReversa);
        }
        try {
            PolizaDto poliza = contabilidad.obtenerPoliza(id);
            Map<UUID, CuentaDto> cuentas = contabilidad.listarCuentas().stream()
                    .collect(Collectors.toMap(CuentaDto::id, c -> c, (a, b) -> a));
            List<MovimientoVista> movimientos = poliza.movimientos().stream()
                    .map(m -> vista(m, cuentas))
                    .toList();
            modelo.addAttribute("poliza", poliza);
            modelo.addAttribute("movimientos", movimientos);
        } catch (HttpClientErrorException.Forbidden e) {
            modelo.addAttribute("error", "Tu rol no tiene acceso a las pólizas contables.");
        } catch (HttpClientErrorException.NotFound e) {
            modelo.addAttribute("error", "La póliza no existe.");
        } catch (RestClientException e) {
            modelo.addAttribute("error", "No se pudo consultar la póliza. Intenta más tarde.");
        }
        return "polizas/detalle";
    }

    /** Pide la reversa de una póliza. Solo para contadores. Redirige al detalle con el resultado. */
    @PostMapping("/{id}/reversa")
    public String reversar(@PathVariable UUID id) {
        try {
            contabilidad.reversarPoliza(id);
            return "redirect:/polizas/" + id + "?revertida=true";
        } catch (HttpClientErrorException e) {
            return "redirect:/polizas/" + id + "?errorReversa=" + codificar(mensajeReversa(e.getStatusCode().value()));
        } catch (RestClientException e) {
            return "redirect:/polizas/" + id + "?errorReversa="
                    + codificar("Contabilidad no está disponible. Intenta más tarde.");
        }
    }

    /** Codifica el texto para usarlo en una URL. */
    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    /** Traduce el estado de la reversa en un mensaje para el usuario. */
    private static String mensajeReversa(int estado) {
        return switch (estado) {
            case 403 -> "Tu rol no puede revertir pólizas.";
            case 404 -> "La póliza no existe.";
            case 409 -> "La póliza no se puede revertir (ya fue revertida o es una reversa).";
            default -> "No se pudo revertir la póliza.";
        };
    }

    /** Indica si el usuario tiene el rol de contador. */
    private static boolean esContador(Authentication usuario) {
        return usuario.getAuthorities().stream().anyMatch(a -> "ROLE_CONTADOR".equals(a.getAuthority()));
    }

    /** Convierte un movimiento en la fila que se muestra, con el nombre de su cuenta. */
    private static MovimientoVista vista(PolizaDto.MovimientoDto m, Map<UUID, CuentaDto> cuentas) {
        CuentaDto cuenta = cuentas.get(m.cuentaId());
        if (cuenta == null) {
            return new MovimientoVista("?", "Cuenta desconocida", m.cargo(), m.abono());
        }
        return new MovimientoVista(cuenta.codigo(), cuenta.nombre(), m.cargo(), m.abono());
    }

    /** Valida el periodo AAAA-MM. Si no es válido, usa el mes actual. */
    private static String periodoValido(String periodo) {
        String actual = YearMonth.now(ZONA).toString();
        if (periodo == null || periodo.isBlank()) {
            return actual;
        }
        try {
            return YearMonth.parse(periodo).toString();
        } catch (DateTimeParseException e) {
            return actual;
        }
    }

    /**
     * Movimiento listo para mostrar: incluye el código y nombre de la cuenta.
     *
     * @param codigo componente de tipo {@code String}.
     * @param nombre componente de tipo {@code String}.
     * @param cargo componente de tipo {@code BigDecimal}.
     * @param abono componente de tipo {@code BigDecimal}.
     */
    public record MovimientoVista(String codigo, String nombre, BigDecimal cargo, BigDecimal abono) {
    }
}
