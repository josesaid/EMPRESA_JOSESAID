package com.mx.josesaid.develop.ui.ia;

import static java.util.Map.entry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /** Catálogo fijo de sugerencias por opción del menú, con ideas y pasos guiados. Son textos de ayuda, no datos del negocio. */
    static final Map<String, List<Idea>> CATALOGO = Map.ofEntries(
            entry("ADMIN", List.of(
                    idea("Revisar los datos de cada usuario para detectar cuentas que no correspondan a su puesto.",
                            "En el menú lateral, haz clic en Accesos > Usuarios.",
                            "Haz clic en la fila de un usuario para abrir su detalle.",
                            "Revisa el correo, el nombre y los datos del usuario.",
                            "Haz clic en Volver para regresar al listado y revisar el siguiente."),
                    idea("Confirmar que el listado de usuarios incluya a todo tu equipo.",
                            "En el menú lateral, haz clic en Accesos > Usuarios.",
                            "Recorre la lista completa; este listado no tiene filtros.",
                            "Si falta alguien, pide a Nexus que prepare su alta."))),
            entry("DIRECCION", List.of(
                    idea("Revisar los indicadores de un periodo y de un área para detectar desviaciones.",
                            "En el menú lateral, haz clic en Dirección > Indicadores.",
                            "En el campo Periodo, escribe el mes con formato AAAA-MM, por ejemplo 2026-10.",
                            "Si quieres ver una sola área, escríbela en el campo Área.",
                            "Haz clic en Consultar y revisa la tabla."),
                    idea("Revisar el reporte de un periodo antes de la reunión de dirección.",
                            "En el menú lateral, haz clic en Dirección > Reporte por periodo.",
                            "En el campo Periodo, escribe el periodo y haz clic en Consultar.",
                            "Revisa los datos que aparecen en el detalle del reporte."))),
            entry("FINANZAS", List.of(
                    idea("Revisar los comprobantes de un folio para confirmar su estado y su total.",
                            "En el menú lateral, haz clic en Finanzas > Comprobantes.",
                            "En el campo Folio, escribe el folio y haz clic en Consultar.",
                            "Haz clic en la fila del comprobante para abrir su detalle.",
                            "Haz clic en Volver para regresar al listado."),
                    idea("Revisar las cuentas contables activas antes de proponer una póliza.",
                            "En el menú lateral, haz clic en Finanzas > Cuentas contables.",
                            "Revisa la columna Activa y usa solo las cuentas activas.",
                            "Anota el código y la naturaleza de las cuentas que vayas a usar.",
                            "Haz clic en Finanzas > Proponer póliza para capturar la operación."))),
            entry("CONTADOR", List.of(
                    idea("Proponer una póliza a partir de la descripción de un movimiento bancario.",
                            "En el menú lateral, haz clic en Finanzas > Proponer póliza.",
                            "En el cuadro Describe la operación, escribe el monto, la moneda y las cuentas del movimiento.",
                            "Haz clic en el botón Proponer póliza.",
                            "En Propuesta pendiente, revisa el resumen y anota el Código de confirmación.",
                            "Escribe ese código en Código de 8 dígitos y haz clic en Confirmar y registrar. Si la propuesta no sirve, haz clic en Cancelar propuesta."),
                    idea("Revisar las pólizas de un periodo y que sus cargos y abonos cuadren.",
                            "En el menú lateral, haz clic en Finanzas > Pólizas.",
                            "En el campo Periodo (AAAA-MM), escribe el mes, por ejemplo 2026-10, y haz clic en Consultar.",
                            "Haz clic en Ver en la fila de la póliza que quieras revisar.",
                            "Compara los totales de Cargo y Abono de la tabla de movimientos.",
                            "Para salir sin cambios, haz clic en Finanzas > Pólizas en el menú lateral."))),
            entry("VENTAS", List.of(
                    idea("Resumir el historial de un cliente revisando sus interacciones antes de una llamada.",
                            "En el menú lateral, haz clic en Comercial > Clientes.",
                            "En el campo RFC o en el campo Nombre, escribe el dato del cliente y haz clic en Consultar.",
                            "Haz clic en la fila del cliente para abrir su ficha.",
                            "Revisa la sección Interacciones de la ficha.",
                            "Haz clic en Volver para regresar al listado."),
                    idea("Dar seguimiento a los pedidos confirmados que aún no se han entregado.",
                            "En el menú lateral, haz clic en Comercial > Pedidos.",
                            "En el campo Estatus, escribe CONFIRMADO y haz clic en Consultar.",
                            "Haz clic en la fila de un pedido para ver su detalle.",
                            "Haz clic en Volver para regresar a la lista."))),
            entry("COMPRAS", List.of(
                    idea("Revisar las órdenes de compra en BORRADOR para aprobarlas o corregirlas.",
                            "En el menú lateral, haz clic en Compras > Órdenes de compra.",
                            "En el campo Estatus, escribe BORRADOR y haz clic en Consultar.",
                            "Haz clic en la fila de la orden para ver sus líneas.",
                            "Haz clic en Volver para regresar al listado."),
                    idea("Revisar los datos de un proveedor antes de hacerle una compra.",
                            "En el menú lateral, haz clic en Compras > Proveedores.",
                            "En el campo Razón social, escribe el nombre y haz clic en Consultar.",
                            "Haz clic en la fila del proveedor para ver sus datos.",
                            "Haz clic en Volver para regresar al listado."))),
            entry("ALMACEN", List.of(
                    idea("Revisar la existencia de los artículos que están bajo mínimo.",
                            "En el menú lateral, haz clic en Inventario > Bajo mínimo.",
                            "Anota el artículo que quieres revisar; si la tabla muestra su ID, cópialo.",
                            "En el menú lateral, haz clic en Inventario > Existencias.",
                            "En el campo ID de artículo, escribe el ID y haz clic en Consultar."),
                    idea("Revisar los movimientos de un artículo para entender por qué cambió su existencia.",
                            "En el menú lateral, haz clic en Inventario > Movimientos.",
                            "En el campo ID de artículo, escribe el ID y haz clic en Consultar.",
                            "Revisa los tipos de movimiento y sus cantidades.",
                            "Compara el resultado con Inventario > Existencias para el mismo ID."))),
            entry("OPERACIONES", List.of(
                    idea("Revisar la trazabilidad de un lote de producción.",
                            "En el menú lateral, haz clic en Inventario > Trazabilidad de lote.",
                            "En el campo Lote, escribe el número de lote y haz clic en Consultar.",
                            "Revisa los movimientos del lote en la tabla."),
                    idea("Revisar la existencia disponible de un artículo antes de programar una orden.",
                            "En el menú lateral, haz clic en Inventario > Existencias.",
                            "En el campo ID de artículo, escribe el ID y haz clic en Consultar.",
                            "Revisa la cantidad disponible."))),
            entry("CALIDAD", List.of(
                    idea("Revisar los movimientos de un lote con incidencia.",
                            "En el menú lateral, haz clic en Inventario > Trazabilidad de lote.",
                            "En el campo Lote, escribe el lote y haz clic en Consultar.",
                            "Anota los movimientos que necesites investigar."),
                    idea("Revisar los documentos de calidad que vencen en los próximos 30 días.",
                            "En el menú lateral, haz clic en Cumplimiento > Por vencer.",
                            "En el campo Días, escribe 30 y haz clic en Consultar.",
                            "Haz clic en la fila de un documento para ver su detalle."))),
            entry("LEGAL", List.of(
                    idea("Revisar los documentos que vencen en los próximos 60 días para avisar a sus responsables.",
                            "En el menú lateral, haz clic en Cumplimiento > Por vencer.",
                            "En el campo Días, escribe 60 y haz clic en Consultar.",
                            "Haz clic en la fila del documento para ver su detalle.",
                            "Haz clic en Volver para regresar a la lista."),
                    idea("Buscar un contrato por su título y revisar sus datos.",
                            "En el menú lateral, haz clic en Cumplimiento > Documentos.",
                            "En el campo Título, escribe el título y haz clic en Consultar.",
                            "Haz clic en la fila del documento para abrir su detalle.",
                            "Haz clic en Volver para regresar al listado."))),
            entry("JEFE_AREA", List.of(
                    idea("Revisar las ventas netas del mes para la reunión semanal.",
                            "En el menú lateral, haz clic en Comercial > Métricas de ventas.",
                            "En el campo Mes (AAAA-MM), escribe el mes, por ejemplo 2026-10, y haz clic en Consultar.",
                            "Revisa los datos del resumen."),
                    idea("Revisar qué áreas tienen pendientes desde el panel general.",
                            "En el menú lateral, haz clic en Inicio.",
                            "Haz clic en la tarjeta del área que quieres revisar, por ejemplo Compras o Inventario.",
                            "Aplica el filtro que necesites en el listado que se abre."))));

    /** Clase de utilidad con el catálogo; no se instancia. */
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

    /** Opción elegible en "Agiliza con IA". El id es rol-índice dentro del catálogo. */
    public record Opcion(String id, String rol, String texto, List<String> pasos, Tarea tarea) {
    }

    /**
     * Pantalla que abre la opción y el único dato que pide. Si valorFijo no es null, el dato ya viene
     * resuelto y no se pide; si campo es null, solo se ofrece ir a la pantalla.
     */
    public record Tarea(String ruta, String campo, String etiqueta, String ejemplo, String valorFijo) {
    }

    private static final Tarea SIN_DATO_INICIO = new Tarea("/", null, null, null, null);

    /** Tarea de cada opción, por id (rol-índice). Cada id del catálogo debe tener su entrada. */
    static final Map<String, Tarea> TAREAS = Map.ofEntries(
            entry("ADMIN-0", new Tarea("/usuarios", null, null, null, null)),
            entry("ADMIN-1", new Tarea("/usuarios", null, null, null, null)),
            entry("DIRECCION-0", new Tarea("/reportes/indicadores", "periodo", "Periodo (AAAA-MM)", "2026-10", null)),
            entry("DIRECCION-1", new Tarea("/reportes/reporte", "periodo", "Periodo (AAAA-MM)", "2026-10", null)),
            entry("FINANZAS-0", new Tarea("/facturacion/comprobantes", "folio", "Folio", "", null)),
            entry("FINANZAS-1", new Tarea("/cuentas", null, null, null, null)),
            entry("CONTADOR-0", new Tarea("/polizas/nueva", null, null, null, null)),
            entry("CONTADOR-1", new Tarea("/polizas", "periodo", "Periodo (AAAA-MM)", "2026-10", null)),
            entry("VENTAS-0", new Tarea("/clientes", "rfc", "RFC del cliente", "", null)),
            entry("VENTAS-1", new Tarea("/ventas/pedidos", "estatus", "Estatus", "", "CONFIRMADO")),
            entry("COMPRAS-0", new Tarea("/compras/ordenes", "estatus", "Estatus", "", "BORRADOR")),
            entry("COMPRAS-1", new Tarea("/compras/proveedores", "razonSocial", "Razón social", "", null)),
            entry("ALMACEN-0", new Tarea("/inventario/existencias", "articulo", "ID de artículo", "", null)),
            entry("ALMACEN-1", new Tarea("/inventario/movimientos", "articulo", "ID de artículo", "", null)),
            entry("OPERACIONES-0", new Tarea("/inventario/trazabilidad", "lote", "Lote", "", null)),
            entry("OPERACIONES-1", new Tarea("/inventario/existencias", "articulo", "ID de artículo", "", null)),
            entry("CALIDAD-0", new Tarea("/inventario/trazabilidad", "lote", "Lote", "", null)),
            entry("CALIDAD-1", new Tarea("/documentos/por-vencer", "dias", "Días", "", "30")),
            entry("LEGAL-0", new Tarea("/documentos/por-vencer", "dias", "Días", "", "60")),
            entry("LEGAL-1", new Tarea("/documentos", "titulo", "Título", "", null)),
            entry("JEFE_AREA-0", new Tarea("/ventas/metricas", "mes", "Mes (AAAA-MM)", "2026-10", null)),
            entry("JEFE_AREA-1", new Tarea("/", null, null, null, null)));

    /** Opciones de los roles del usuario, en el mismo orden que el log. */
    public static List<Opcion> opciones(Collection<String> roles) {
        Set<String> textos = new LinkedHashSet<>();
        List<Opcion> lista = new ArrayList<>();
        for (String rol : PRIORIDAD) {
            if (!roles.contains(rol)) {
                continue;
            }
            List<Idea> ideas = CATALOGO.getOrDefault(rol, List.of());
            for (int i = 0; i < ideas.size(); i++) {
                Idea idea = ideas.get(i);
                if (textos.add(idea.texto())) {
                    String id = rol + "-" + i;
                    lista.add(new Opcion(id, rol, idea.texto(), idea.pasos(), TAREAS.getOrDefault(id, SIN_DATO_INICIO)));
                }
            }
        }
        return List.copyOf(lista);
    }

    /** Opción por id, solo si pertenece a los roles del usuario. */
    public static Optional<Opcion> opcion(Collection<String> roles, String id) {
        if (id == null) {
            return Optional.empty();
        }
        return opciones(roles).stream().filter(o -> o.id().equals(id)).findFirst();
    }

    /** Crea una idea con su texto y sus pasos. */
    private static Idea idea(String texto, String... pasos) {
        return new Idea(texto, List.of(pasos));
    }
}
