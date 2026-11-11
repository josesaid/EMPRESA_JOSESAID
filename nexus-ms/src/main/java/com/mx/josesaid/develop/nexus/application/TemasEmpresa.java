package com.mx.josesaid.develop.nexus.application;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Filtro previo al modelo: solo deja pasar preguntas sobre EMPRESA_JOSESAID.
 * Las preguntas ajenas se rechazan sin llamar a Claude, así que no hay consumo de tokens.
 */
public final class TemasEmpresa {

    public static final String RECHAZO = "Solo puedo ayudarte con consultas de EMPRESA_JOSESAID, "
            + "por ejemplo: \"¿Cuántas existencias hay del SKU-001?\" o \"¿Qué comprobantes siguen pendientes de cobro?\".";

    /** Palabras clave de los temas de la empresa: clientes, pedidos, compras, inventario, contabilidad, etc. Una pregunta que no contiene ninguna se rechaza antes de llamar al modelo. */
    private static final Pattern TEMAS = Pattern.compile(
            "\\b(clientes?|rfc|usuarios?|accesos?|roles?|factur\\w*|comprobantes?|cobros?|cobr\\w*|pagos?|pagar"
            + "|cuentas?|saldos?|cartera|polizas?|contab\\w*|periodos?|compras?|ordenes?|proveedor\\w*|pedidos?"
            + "|ventas?|cotizaci\\w*|inventario\\w*|existencias?|sku\\w*|almacen\\w*|insumos?|documentos?"
            + "|contratos?|cumplimiento|reportes?|indicadores?|politicas?|empresa|emitid\\w*|pendientes?"
            + "|aprob\\w*|cancel\\w*|reversas?)\\b");

    /** Saludos y ayuda, solo cuando son el mensaje completo. */
    private static final Pattern SALUDO = Pattern.compile(
            "^[\\s!¡?¿.,]*(hola|buenos dias|buenas tardes|buenas noches|ayuda|que puedes hacer)[\\s!¡?¿.,]*$");

    /** Un identificador UUID es dato de inventario: se deja pasar aunque la frase no tenga tema. */
    private static final Pattern UUID = Pattern.compile(
            "\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b");

    /** Clase de utilidad: no se instancia. */
    private TemasEmpresa() {
    }

    /** Indica si el texto habla de un tema de la empresa, buscando las palabras clave sin distinguir mayúsculas. */
    public static boolean esDeEmpresa(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return SALUDO.matcher(normalizado).matches()
                || TEMAS.matcher(normalizado).find()
                || UUID.matcher(normalizado).find();
    }
}
