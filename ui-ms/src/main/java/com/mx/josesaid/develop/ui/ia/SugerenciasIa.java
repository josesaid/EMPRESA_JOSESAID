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
                            "En el menú lateral, haz clic en Accesos > Usuarios.",
                            "Haz clic en el botón \"Bitácora de accesos\" (nuevo), arriba de la tabla.",
                            "En el filtro \"Evento\", elige \"Intentos fallidos\" (nuevo) y haz clic en Consultar.",
                            "Haz clic en un usuario para ver sus intentos y la IP de origen."),
                    idea("Preparar altas de usuarios con roles sugeridos según el área de cada persona.",
                            "En el menú lateral, haz clic en Accesos > Usuarios.",
                            "Abre el alta pendiente de Nexus con el botón \"Completar alta\".",
                            "En el campo Roles, revisa la sugerencia \"Rol sugerido\" (nueva) junto al área.",
                            "Haz clic en \"Aceptar sugerencia\" o elige otro rol y después en \"Crear usuario\"."))),
            entry("DIRECCION", List.of(
                    idea("Generar un resumen semanal de indicadores por área con las desviaciones más importantes.",
                            "En el menú lateral, haz clic en Dirección > Indicadores.",
                            "En el filtro \"Periodo\", elige \"Semana\" y haz clic en Consultar.",
                            "Haz clic en el botón \"Generar resumen con IA\" (nuevo), arriba de la tabla.",
                            "Elige \"Todas las áreas\" o un área y haz clic en \"Generar\"."),
                    idea("Explicar en lenguaje claro las variaciones del reporte por periodo.",
                            "En el menú lateral, haz clic en Dirección > Reporte por periodo.",
                            "Elige el periodo y haz clic en Consultar.",
                            "Haz clic en el botón \"Explicar variaciones\" (nuevo), debajo de la tabla.",
                            "Lee la explicación en el recuadro que aparece."))),
            entry("FINANZAS", List.of(
                    idea("Proponer la clasificación contable de los comprobantes nuevos a partir de su descripción.",
                            "En el menú lateral, haz clic en Finanzas > Comprobantes.",
                            "Haz clic en una fila cuyo campo \"Cuenta contable\" esté vacío.",
                            "Haz clic en el botón \"Sugerir cuenta\" (nuevo), junto al campo Cuenta contable.",
                            "Elige la cuenta sugerida en el listado y haz clic en Guardar."),
                    idea("Priorizar pagos a proveedores vencidos según importe y antigüedad.",
                            "En el menú lateral, haz clic en Finanzas > Pagos a proveedores.",
                            "En el filtro \"Estado\", elige \"Vencidos\" (nuevo) y haz clic en Consultar.",
                            "Haz clic en el encabezado \"Prioridad\" (nuevo) para ordenar por importe y días de atraso."))),
            entry("CONTADOR", List.of(
                    idea("Sugerir pólizas a partir de movimientos bancarios y revisar cuentas con saldos atípicos.",
                            "En el menú lateral, haz clic en Finanzas > Proponer póliza.",
                            "En el cuadro \"Describe la operación\", escribe el monto, la moneda y las cuentas del movimiento bancario.",
                            "Haz clic en el botón \"Proponer póliza\".",
                            "Revisa la propuesta de cargos y abonos antes de registrarla."),
                    idea("Conciliar movimientos bancarios con coincidencias aproximadas de importe y fecha.",
                            "En el menú lateral, haz clic en Finanzas > Cuentas contables.",
                            "Elige la cuenta bancaria y haz clic en el botón \"Conciliar\" (nuevo).",
                            "Marca la casilla de cada coincidencia sugerida y haz clic en \"Confirmar conciliación\" (nuevo)."))),
            entry("VENTAS", List.of(
                    idea("Resumir el historial de interacciones de un cliente antes de una llamada.",
                            "En el menú lateral, haz clic en Comercial > Clientes.",
                            "Busca el cliente por RFC o nombre y haz clic en su fila.",
                            "En la sección Interacciones, haz clic en el botón \"Resumir\" (nuevo).",
                            "Lee el resumen de cinco puntos que aparece arriba de la lista."),
                    idea("Redactar cotizaciones y correos de seguimiento a partir de los datos del cliente.",
                            "En el menú lateral, haz clic en Comercial > Pedidos.",
                            "Haz clic en el pedido o cotización para abrir su ficha.",
                            "Haz clic en el botón \"Redactar correo\" (nuevo) y elige el tipo \"Cotización\" o \"Seguimiento\".",
                            "Revisa el borrador y cópialo a tu correo. El sistema no lo envía."))),
            entry("COMPRAS", List.of(
                    idea("Comparar cotizaciones de proveedores por precio, plazo y condiciones.",
                            "En el menú lateral, haz clic en Compras > Órdenes de compra.",
                            "Haz clic en la orden en estatus BORRADOR para abrirla.",
                            "Haz clic en el botón \"Comparar proveedores\" (nuevo).",
                            "Marca los proveedores a comparar, haz clic en \"Comparar\" y revisa la recomendación."),
                    idea("Anticipar reabastecimientos a partir del consumo y de los artículos cerca del mínimo.",
                            "En el menú lateral, haz clic en Inventario > Bajo mínimo.",
                            "Marca la casilla de los artículos con cobertura baja.",
                            "Haz clic en el botón \"Proponer orden de compra\" (nuevo).",
                            "Revisa cantidades y proveedor, y haz clic en \"Crear orden\" para dejarla en BORRADOR."))),
            entry("ALMACEN", List.of(
                    idea("Anticipar artículos que llegarán a bajo mínimo con base en su consumo reciente.",
                            "En el menú lateral, haz clic en Inventario > Bajo mínimo.",
                            "Elige la opción \"Próximos a bajo mínimo\" (nueva) arriba de la tabla.",
                            "Haz clic en el encabezado \"Días de cobertura\" (nueva columna) para ordenar de menor a mayor."),
                    idea("Explicar las diferencias entre cantidad ordenada y recibida en cada recepción.",
                            "En el menú lateral, haz clic en Compras > Órdenes de compra.",
                            "Abre una orden en estatus RECIBIDA.",
                            "Haz clic en el botón \"Explicar diferencia\" (nuevo) junto a la línea con faltante."))),
            entry("OPERACIONES", List.of(
                    idea("Sugerir la programación de órdenes de operación según la demanda prevista.",
                            "En el menú principal, abre el módulo Operaciones (aún no existe en el menú).",
                            "Haz clic en el botón \"Programar con IA\" (nuevo) en la lista de órdenes de operación.",
                            "Revisa el calendario propuesto y elige \"Aprobar\" o \"Ajustar\"."))),
            entry("CALIDAD", List.of(
                    idea("Agrupar las causas de rechazo para detectar patrones repetidos.",
                            "En el menú principal, abre el módulo Calidad (aún no existe en el menú).",
                            "En la lista de rechazos, elige la opción \"Agrupar causas\" (nueva).",
                            "Haz clic en el grupo con más casos para ver sus registros."))),
            entry("LEGAL", List.of(
                    idea("Revisar vencimientos y cláusulas clave en contratos y documentos.",
                            "En el menú lateral, haz clic en Cumplimiento > Documentos.",
                            "Haz clic en el contrato para abrir su ficha.",
                            "Haz clic en el botón \"Extraer cláusulas\" (nuevo).",
                            "Revisa la lista de cláusulas de renovación y penalización."))),
            entry("JEFE_AREA", List.of(
                    idea("Resumir los pendientes del área para la reunión semanal.",
                            "En el menú lateral, haz clic en Inicio.",
                            "Haz clic en la tarjeta \"Resumen de pendientes\" (nueva) del panel general.",
                            "Elige el área y el periodo \"Últimos 7 días\", y haz clic en \"Generar\"."))));

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
