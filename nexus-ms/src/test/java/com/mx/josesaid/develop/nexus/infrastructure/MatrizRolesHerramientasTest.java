package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Matriz de escenarios: qué herramienta puede usar cada rol desde Nexus.
 * Fija el alcance por rol; si cambia el mapa de roles, esta prueba debe cambiar a propósito.
 */
class MatrizRolesHerramientasTest {

    @BeforeEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cerrar() {
        SecurityContextHolder.clearContext();
    }

    private static void como(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u", null, roles));
    }

    @ParameterizedTest(name = "{0} puede usar {1}: {2}")
    @CsvSource({
            // Dirección: reportes, políticas, contabilidad en lectura; no escribe pólizas ni pedidos.
            "ROLE_DIRECCION, consultar_indicadores, true",
            "ROLE_DIRECCION, resumen_ventas, true",
            "ROLE_DIRECCION, consultar_politicas, true",
            "ROLE_DIRECCION, consultar_poliza, true",
            "ROLE_DIRECCION, confirmar_pedido, false",
            "ROLE_DIRECCION, sugerir_poliza, false",
            "ROLE_DIRECCION, buscar_clientes, false",
            // Ventas: clientes, pedidos y acciones de venta; sin políticas ni contabilidad.
            "ROLE_VENTAS, buscar_clientes, true",
            "ROLE_VENTAS, confirmar_pedido, true",
            "ROLE_VENTAS, confirmar_accion, true",
            "ROLE_VENTAS, consultar_politicas, false",
            "ROLE_VENTAS, sugerir_poliza, false",
            "ROLE_VENTAS, consultar_poliza, false",
            "ROLE_VENTAS, consultar_reporte, false",
            // Contador: pólizas y confirmación MCP; no políticas ni acciones de venta.
            "ROLE_CONTADOR, consultar_poliza, true",
            "ROLE_CONTADOR, sugerir_poliza, true",
            "ROLE_CONTADOR, confirmar_accion, true",
            "ROLE_CONTADOR, consultar_politicas, false",
            "ROLE_CONTADOR, confirmar_pedido, false",
            "ROLE_CONTADOR, buscar_clientes, false",
            // Compras: políticas y órdenes de compra; no contabilidad ni ventas.
            "ROLE_COMPRAS, consultar_politicas, true",
            "ROLE_COMPRAS, consultar_orden_compra, true",
            "ROLE_COMPRAS, buscar_proveedores, true",
            "ROLE_COMPRAS, consultar_poliza, false",
            "ROLE_COMPRAS, sugerir_poliza, false",
            "ROLE_COMPRAS, confirmar_pedido, false",
            // Almacén, legal, finanzas y administración.
            "ROLE_ALMACEN, consultar_existencias, true",
            "ROLE_ALMACEN, consultar_orden_compra, true",
            "ROLE_ALMACEN, consultar_politicas, false",
            "ROLE_ALMACEN, sugerir_poliza, false",
            "ROLE_LEGAL, consultar_documento, true",
            "ROLE_LEGAL, consultar_reporte, false",
            "ROLE_LEGAL, consultar_politicas, false",
            "ROLE_FINANZAS, consultar_comprobante, true",
            "ROLE_FINANZAS, resumen_ventas, true",
            "ROLE_FINANZAS, consultar_poliza, false",
            "ROLE_FINANZAS, sugerir_poliza, false",
            "ROLE_ADMIN, buscar_clientes, true",
            "ROLE_ADMIN, consultar_usuario, true",
            "ROLE_ADMIN, consultar_politicas, false",
            "ROLE_ADMIN, confirmar_accion, false",
    })
    void cadaRolVeSoloSuAlcance(String rol, String herramienta, boolean permitida) {
        como(rol);

        assertThat(AlcanceHerramientas.puedeUsar(herramienta)).isEqualTo(permitida);
    }

    @ParameterizedTest(name = "rol no reconocido {0} no puede usar {1}")
    @CsvSource({
            "ROLE_JEFE_AREA, consultar_politicas",
            "ROLE_JEFE_AREA, consultar_reporte",
            "ROLE_OPERACIONES, sugerir_poliza",
            "ROLE_OPERACIONES, confirmar_pedido",
    })
    void rolesNoMapeadosSeNiegan(String rol, String herramienta) {
        como(rol);

        assertThat(AlcanceHerramientas.puedeUsar(herramienta)).isFalse();
    }

    @ParameterizedTest(name = "sin identidad no puede usar {0}")
    @CsvSource({"consultar_politicas", "consultar_reporte", "sugerir_poliza", "confirmar_accion"})
    void sinIdentidadNadaSeOfrece(String herramienta) {
        assertThat(AlcanceHerramientas.puedeUsar(herramienta)).isFalse();
    }

    @ParameterizedTest(name = "herramienta inexistente {0} se niega aun con rol de dirección")
    @CsvSource({"herramienta_inventada", "borrar_todo", "exportar_base"})
    void herramientasInexistentesSeNiegan(String herramienta) {
        como("ROLE_DIRECCION");

        assertThat(AlcanceHerramientas.puedeUsar(herramienta)).isFalse();
    }

    @ParameterizedTest(name = "usuario con {0} y {1} hereda la unión de ambos alcances: {2}")
    @CsvSource({
            "ROLE_CONTADOR, ROLE_COMPRAS, consultar_politicas, true",
            "ROLE_CONTADOR, ROLE_COMPRAS, sugerir_poliza, true",
            "ROLE_CONTADOR, ROLE_COMPRAS, confirmar_pedido, false",
    })
    void unUsuarioConVariosRolesSumaSusAlcances(String rolA, String rolB, String herramienta, boolean permitida) {
        como(rolA, rolB);

        assertThat(AlcanceHerramientas.puedeUsar(herramienta)).isEqualTo(permitida);
    }
}
