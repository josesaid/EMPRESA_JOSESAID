package com.mx.josesaid.develop.ui.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IP del navegador que hizo la solicitud, para el límite de intentos del gateway.
 * Usa X-Forwarded-For solo si la conexión viene de un proxy de confianza (UI_PROXIES_CONFIABLES,
 * por ejemplo el ingress). En cualquier otro caso usa la IP directa, para que el navegador no pueda falsearla.
 */
@Component
public class IpOrigen {

    private final List<String> proxiesConfiables;

    public IpOrigen(@Value("${ui.proxies-confiables:}") String proxies) {
        this.proxiesConfiables = Arrays.asList(proxies.split(","));
    }

    /** IP del cliente de la solicitud actual, o null si no hay solicitud web en este hilo. */
    public String actual() {
        var atributos = RequestContextHolder.getRequestAttributes();
        if (!(atributos instanceof ServletRequestAttributes servlet)) {
            return null;
        }
        return resolver(servlet.getRequest());
    }

    String resolver(HttpServletRequest request) {
        String directa = request.getRemoteAddr();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank() || !esDeConfianza(directa)) {
            return directa;
        }
        String candidata = xff.split(",")[0].trim();
        return esIpValida(candidata) ? candidata : directa;
    }

    boolean esDeConfianza(String ip) {
        for (String rango : proxiesConfiables) {
            if (!rango.isBlank() && perteneceAlRango(ip, rango.trim())) {
                return true;
            }
        }
        return false;
    }

    static boolean perteneceAlRango(String ip, String cidr) {
        String[] partes = cidr.split("/");
        if (partes.length != 2 || !esIpValida(ip)) {
            throw new IllegalArgumentException("Rango o IP inválidos: " + cidr + " " + ip);
        }
        byte[] base = literal(partes[0]);
        byte[] candidata = literal(ip);
        if (base.length != candidata.length) {
            return false;
        }
        int prefijo = Integer.parseInt(partes[1]);
        if (prefijo < 0 || prefijo > base.length * 8) {
            throw new IllegalArgumentException("Prefijo CIDR fuera de rango: " + cidr);
        }
        for (int i = 0; i < base.length; i++) {
            int bits = Math.min(8, Math.max(0, prefijo - i * 8));
            int mascara = bits == 0 ? 0 : (0xFF << (8 - bits)) & 0xFF;
            if ((base[i] & mascara) != (candidata[i] & mascara)) {
                return false;
            }
        }
        return true;
    }

    static boolean esIpValida(String s) {
        return !s.isEmpty() && s.length() <= 45 && s.matches("[0-9a-fA-F:.]+") && (s.contains(".") || s.contains(":"));
    }

    private static byte[] literal(String ip) {
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("IP inválida: " + ip, e);
        }
    }
}
