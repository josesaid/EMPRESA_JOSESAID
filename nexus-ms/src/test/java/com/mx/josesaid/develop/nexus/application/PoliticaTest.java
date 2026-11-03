package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class PoliticaTest {

    private static final String CONTENIDO = """
            ---
            titulo: Política de aprobación de órdenes de compra
            vigencia: 2026-11-01
            roles: [compras, DIRECCION]
            ---

            # Política

            1. Toda orden la crea COMPRAS.
            """;

    @Test
    void leeFrontMatterYCuerpo() {
        Politica politica = Politica.desde("aprobacion-ordenes-compra", CONTENIDO);

        assertThat(politica.id()).isEqualTo("aprobacion-ordenes-compra");
        assertThat(politica.titulo()).isEqualTo("Política de aprobación de órdenes de compra");
        assertThat(politica.vigencia()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(politica.roles()).containsExactly("COMPRAS", "DIRECCION");
        assertThat(politica.cuerpo()).contains("1. Toda orden la crea COMPRAS.");
    }

    @Test
    void rechazaPoliticaSinFrontMatter() {
        assertThatThrownBy(() -> Politica.desde("x", "# Sin front matter"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("front matter");
    }

    @Test
    void rechazaPoliticaSinRoles() {
        String sinRoles = "---\ntitulo: T\nvigencia: 2026-11-01\nroles: []\n---\nTexto";

        assertThatThrownBy(() -> Politica.desde("x", sinRoles))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roles");
    }

    @Test
    void rolesVacioEnRecordNoSeAcepta() {
        assertThatThrownBy(() -> new Politica("x", "T", LocalDate.now(), List.of(), "Texto"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
