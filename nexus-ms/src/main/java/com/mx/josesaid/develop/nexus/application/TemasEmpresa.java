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

    private static final Pattern TEMAS = Pattern.compile(
            "\\b(clientes?|rfc|usuarios?|accesos?|roles?|factur\\w*|comprobantes?|cobros?|cobr\\w*|pagos?|pagar"
            + "|cuentas?|saldos?|cartera|polizas?|contab\\w*|periodos?|compras?|ordenes?|proveedor\\w*|pedidos?"
            + "|ventas?|cotizaci\\w*|inventario\\w*|existencias?|sku\\w*|almacen\\w*|insumos?|documentos?"
            + "|contratos?|cumplimiento|reportes?|indicadores?|politicas?|empresa|emitid\\w*|pendientes?"
            + "|aprob\\w*|cancel\\w*|reversas?)\\b");

    /** Saludos y ayuda, solo cuando son el mensaje completo. */
    private static final Pattern SALUDO = Pattern.compile(
            "^[\\s!¡?¿.,]*(hola|buenos dias|buenas tardes|buenas noches|ayuda|que puedes hacer)[\\s!¡?¿.,]*$");

    private TemasEmpresa() {
    }

    public static boolean esDeEmpresa(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return SALUDO.matcher(normalizado).matches() || TEMAS.matcher(normalizado).find();
    }
}
