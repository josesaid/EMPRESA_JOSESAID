package com.mx.josesaid.develop.nexus.comun.prompts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RepositorioPromptsTest {

    private final RepositorioPrompts repositorio = new RepositorioPrompts();

    @Test
    void cargaLaVersionVigenteDelIndice() {
        PromptVersionado prompt = repositorio.obtener("asistente-base");

        assertThat(prompt.nombre()).isEqualTo("asistente-base");
        assertThat(prompt.version()).isEqualTo(1);
        assertThat(prompt.texto()).contains("Eres Nexus").contains("Responde siempre en español");
    }

    @Test
    void cadaVersionVigenteTieneArchivoNoVacio() {
        repositorio.versionesVigentes().forEach((nombre, version) -> {
            PromptVersionado prompt = repositorio.obtener(nombre, version);
            assertThat(prompt.texto()).as(nombre + " v" + version).isNotBlank();
        });
    }

    @Test
    void versionInexistenteFalla() {
        assertThatThrownBy(() -> repositorio.obtener("asistente-base", 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void promptDesconocidoFalla() {
        assertThatThrownBy(() -> repositorio.obtener("no-existe"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
