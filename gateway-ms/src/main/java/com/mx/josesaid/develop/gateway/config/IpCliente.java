package com.mx.josesaid.develop.gateway.config;

import java.net.InetAddress;
import java.util.List;

/**
 * Determina la IP del cliente para los límites de intentos.
 * Usa X-Forwarded-For solo si la conexión viene de un proxy de confianza (por ejemplo ui-ms).
 * En cualquier otro caso usa la IP directa, para que nadie falsee su IP desde fuera.
 */
public final class IpCliente {

    /** Clase de utilidad: no se instancia. */
    private IpCliente() {
    }

    /**
     * @param ipDirecta IP de la conexión que llegó al gateway
     * @param xForwardedFor valor del encabezado, o null
     * @param proxiesConfiables rangos CIDR de los proxies de confianza; vacío = no confiar en ninguno
     */
    public static String resolver(String ipDirecta, String xForwardedFor, List<String> proxiesConfiables) {
        if (ipDirecta == null || xForwardedFor == null || xForwardedFor.isBlank()) {
            return ipDirecta == null ? "desconocida" : ipDirecta;
        }
        if (!esDeConfianza(ipDirecta, proxiesConfiables)) {
            return ipDirecta;
        }
        String candidata = xForwardedFor.split(",")[0].trim();
        return esIpValida(candidata) ? candidata : ipDirecta;
    }

    /** Indica si la IP pertenece a alguno de los rangos CIDR de proxies de confianza. Un rango vacío se ignora. */
    static boolean esDeConfianza(String ip, List<String> proxies) {
        for (String rango : proxies) {
            if (!rango.isBlank() && perteneceAlRango(ip, rango.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Compara una IP literal contra un rango CIDR. Lanza IllegalArgumentException si el rango no es válido. */
    static boolean perteneceAlRango(String ip, String cidr) {
        String[] partes = cidr.split("/");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Rango CIDR inválido: " + cidr);
        }
        byte[] base = literal(partes[0]);
        byte[] candidata = literal(ip);
        if (base.length != candidata.length) {
            return false;
        }
        int prefijo;
        try {
            prefijo = Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prefijo CIDR inválido: " + cidr, e);
        }
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

    /** Convierte una IP literal en sus bytes para compararla contra un rango CIDR. No hace búsqueda DNS. */
    private static byte[] literal(String ip) {
        if (!esIpValida(ip)) {
            throw new IllegalArgumentException("IP inválida: " + ip);
        }
        try {
            // Solo literales: no hace búsqueda DNS porque la cadena ya fue validada como IP.
            return InetAddress.getByName(ip).getAddress();
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("IP inválida: " + ip, e);
        }
    }

    /** Solo caracteres de IPv4 o IPv6 literal; no hace búsqueda DNS. */
    static boolean esIpValida(String s) {
        return !s.isEmpty() && s.length() <= 45 && s.matches("[0-9a-fA-F:.]+") && (s.contains(".") || s.contains(":"));
    }
}
