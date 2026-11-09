package com.mx.josesaid.develop.ui.ia;

import static java.util.Map.entry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sugerencias fijas de dónde aplicar IA, por rol. Se eligen según los roles del usuario en la sesión.
 * Son ideas para priorizar, no funciones que ya existan en el sistema.
 */
public final class SugerenciasIa {

    /** Una línea del log: hora de generación, rol al que aplica y texto. */
    public record Sugerencia(String hora, String rol, String texto) {
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Mismo orden que el color de la barra: si hay varios roles, primero los de mayor prioridad. */
    static final List<String> PRIORIDAD = List.of(
            "ADMIN", "DIRECCION", "FINANZAS", "CONTADOR", "VENTAS", "COMPRAS",
            "ALMACEN", "OPERACIONES", "CALIDAD", "LEGAL", "JEFE_AREA");

    static final Map<String, List<String>> CATALOGO = Map.ofEntries(
            entry("ADMIN", List.of(
                    "Revisar la bitácora de accesos para detectar intentos fallidos repetidos y bloqueos inusuales.",
                    "Preparar altas de usuarios con roles sugeridos según el área de cada persona.")),
            entry("DIRECCION", List.of(
                    "Generar un resumen semanal de indicadores por área con las desviaciones más importantes.",
                    "Explicar en lenguaje claro las variaciones del reporte por periodo.")),
            entry("FINANZAS", List.of(
                    "Proponer la clasificación contable de los comprobantes nuevos a partir de su descripción.",
                    "Priorizar pagos a proveedores vencidos según importe y antigüedad.")),
            entry("CONTADOR", List.of(
                    "Sugerir pólizas a partir de movimientos bancarios y revisar cuentas con saldos atípicos.",
                    "Conciliar movimientos bancarios con coincidencias aproximadas de importe y fecha.")),
            entry("VENTAS", List.of(
                    "Resumir el historial de interacciones de un cliente antes de una llamada.",
                    "Redactar cotizaciones y correos de seguimiento a partir de los datos del cliente.")),
            entry("COMPRAS", List.of(
                    "Comparar cotizaciones de proveedores por precio, plazo y condiciones.",
                    "Anticipar reabastecimientos a partir del consumo y de los artículos cerca del mínimo.")),
            entry("ALMACEN", List.of(
                    "Anticipar artículos que llegarán a bajo mínimo con base en su consumo reciente.",
                    "Explicar las diferencias entre cantidad ordenada y recibida en cada recepción.")),
            entry("OPERACIONES", List.of(
                    "Sugerir la programación de órdenes de operación según la demanda prevista.")),
            entry("CALIDAD", List.of(
                    "Agrupar las causas de rechazo para detectar patrones repetidos.")),
            entry("LEGAL", List.of(
                    "Revisar vencimientos y cláusulas clave en contratos y documentos.")),
            entry("JEFE_AREA", List.of(
                    "Resumir los pendientes del área para la reunión semanal.")));

    private SugerenciasIa() {
    }

    /** Sugerencias de los roles dados, de mayor a menor prioridad, sin repetir textos. */
    public static List<Sugerencia> para(Collection<String> roles, LocalTime ahora) {
        String hora = ahora.format(HORA);
        Set<String> textos = new LinkedHashSet<>();
        List<Sugerencia> log = new ArrayList<>();
        for (String rol : PRIORIDAD) {
            if (!roles.contains(rol)) {
                continue;
            }
            for (String texto : CATALOGO.getOrDefault(rol, List.of())) {
                if (textos.add(texto)) {
                    log.add(new Sugerencia(hora, rol, texto));
                }
            }
        }
        return List.copyOf(log);
    }
}
