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
 * Sugerencias fijas de dónde aplicar IA, por rol, con los pasos para llevarlas a cabo.
 * Se eligen según los roles del usuario en la sesión. Son ideas para priorizar, no funciones que ya existan.
 */
public final class SugerenciasIa {

    /** Idea del catálogo: qué hacer y cómo llevarlo a cabo. */
    public record Idea(String texto, List<String> pasos) {
    }

    /** Una línea del log: hora de generación, rol al que aplica, idea y pasos. */
    public record Sugerencia(String hora, String rol, String texto, List<String> pasos) {
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Mismo orden que el color de la barra: si hay varios roles, primero los de mayor prioridad. */
    static final List<String> PRIORIDAD = List.of(
            "ADMIN", "DIRECCION", "FINANZAS", "CONTADOR", "VENTAS", "COMPRAS",
            "ALMACEN", "OPERACIONES", "CALIDAD", "LEGAL", "JEFE_AREA");

    static final Map<String, List<Idea>> CATALOGO = Map.ofEntries(
            entry("ADMIN", List.of(
                    idea("Revisar la bitácora de accesos para detectar intentos fallidos repetidos y bloqueos inusuales.",
                            "Consultar intentos fallidos y bloqueos de usuarios-accesos (intentos_fallidos, bloqueado_hasta).",
                            "Agrupar por usuario e IP para detectar patrones repetidos.",
                            "Mostrar las alertas en el panel del administrador."),
                    idea("Preparar altas de usuarios con roles sugeridos según el área de cada persona.",
                            "Tomar el área y el puesto del alta preparada en Nexus.",
                            "Proponer roles con una tabla de equivalencias área-rol.",
                            "El administrador confirma antes de crear el usuario."))),
            entry("DIRECCION", List.of(
                    idea("Generar un resumen semanal de indicadores por área con las desviaciones más importantes.",
                            "Leer los indicadores de reportes-ejecutivos-ms de la semana.",
                            "Calcular variaciones contra la semana anterior.",
                            "Pedir a la IA un resumen de cinco líneas con esas variaciones."),
                    idea("Explicar en lenguaje claro las variaciones del reporte por periodo.",
                            "Obtener el reporte del periodo seleccionado y el anterior.",
                            "Enviar ambos al modelo para explicar las diferencias.",
                            "Mostrar la explicación debajo del reporte."))),
            entry("FINANZAS", List.of(
                    idea("Proponer la clasificación contable de los comprobantes nuevos a partir de su descripción.",
                            "Tomar la descripción y el proveedor del comprobante.",
                            "Consultar el catálogo de cuentas contables.",
                            "Proponer la cuenta y que el usuario la confirme."),
                    idea("Priorizar pagos a proveedores vencidos según importe y antigüedad.",
                            "Listar pagos a proveedores con fecha de vencimiento pasada.",
                            "Ordenar por importe y días de atraso.",
                            "Mostrar la lista con el motivo de cada posición."))),
            entry("CONTADOR", List.of(
                    idea("Sugerir pólizas a partir de movimientos bancarios y revisar cuentas con saldos atípicos.",
                            "Leer los movimientos bancarios que no tienen póliza.",
                            "Proponer cargos y abonos según el concepto y las cuentas usadas antes.",
                            "El contador revisa y publica la póliza."),
                    idea("Conciliar movimientos bancarios con coincidencias aproximadas de importe y fecha.",
                            "Emparejar movimientos y registros contables con importe exacto.",
                            "Para los demás, buscar importes con diferencia menor a un umbral y fechas cercanas.",
                            "Dejar las coincidencias sugeridas para confirmación."))),
            entry("VENTAS", List.of(
                    idea("Resumir el historial de interacciones de un cliente antes de una llamada.",
                            "Obtener las interacciones del cliente desde clientes-ms.",
                            "Enviarlas al modelo con la instrucción de resumir en cinco puntos.",
                            "Mostrar el resumen en la ficha del cliente."),
                    idea("Redactar cotizaciones y correos de seguimiento a partir de los datos del cliente.",
                            "Tomar los datos del cliente y las líneas de la cotización.",
                            "Generar un borrador de correo con el modelo.",
                            "El vendedor revisa y lo envía manualmente."))),
            entry("COMPRAS", List.of(
                    idea("Comparar cotizaciones de proveedores por precio, plazo y condiciones.",
                            "Reunir las cotizaciones del mismo artículo de distintos proveedores.",
                            "Normalizar precio, plazo y condiciones en una tabla.",
                            "Pedir a la IA una recomendación con su justificación."),
                    idea("Anticipar reabastecimientos a partir del consumo y de los artículos cerca del mínimo.",
                            "Calcular el consumo de los últimos 30 días por artículo.",
                            "Calcular los días de cobertura con la existencia disponible.",
                            "Proponer orden de compra si la cobertura es menor al plazo del proveedor."))),
            entry("ALMACEN", List.of(
                    idea("Anticipar artículos que llegarán a bajo mínimo con base en su consumo reciente.",
                            "Calcular el consumo diario con las salidas de los últimos 30 días.",
                            "Dividir la existencia disponible entre el consumo diario: son los días de cobertura.",
                            "Avisar cuando la cobertura sea menor al plazo de reposición."),
                    idea("Explicar las diferencias entre cantidad ordenada y recibida en cada recepción.",
                            "Tomar cantidadOrdenada y cantidadRecibida de la orden.",
                            "Calcular la diferencia por línea.",
                            "Pedir a la IA posibles causas y mostrarlas junto a la recepción."))),
            entry("OPERACIONES", List.of(
                    idea("Sugerir la programación de órdenes de operación según la demanda prevista.",
                            "Obtener la demanda prevista por periodo.",
                            "Calcular la capacidad disponible de las órdenes de operación.",
                            "Proponer un calendario que respete la capacidad, para aprobación del responsable."))),
            entry("CALIDAD", List.of(
                    idea("Agrupar las causas de rechazo para detectar patrones repetidos.",
                            "Recopilar las causas de rechazo registradas.",
                            "Agruparlas por similitud de texto.",
                            "Mostrar los grupos más frecuentes para revisión."))),
            entry("LEGAL", List.of(
                    idea("Revisar vencimientos y cláusulas clave en contratos y documentos.",
                            "Listar contratos y documentos con su fecha de vencimiento.",
                            "Extraer con el modelo las cláusulas de renovación y penalización.",
                            "Avisar con anticipación de los vencimientos."))),
            entry("JEFE_AREA", List.of(
                    idea("Resumir los pendientes del área para la reunión semanal.",
                            "Reunir los pendientes del área de los últimos siete días.",
                            "Pedir a la IA un resumen agrupado por responsable.",
                            "Enviar el resumen antes de la reunión semanal."))));

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
            for (Idea idea : CATALOGO.getOrDefault(rol, List.of())) {
                if (textos.add(idea.texto())) {
                    log.add(new Sugerencia(hora, rol, idea.texto(), idea.pasos()));
                }
            }
        }
        return List.copyOf(log);
    }

    private static Idea idea(String texto, String... pasos) {
        return new Idea(texto, List.of(pasos));
    }
}
