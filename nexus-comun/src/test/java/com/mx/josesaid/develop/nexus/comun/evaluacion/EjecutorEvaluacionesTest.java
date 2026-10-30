package com.mx.josesaid.develop.nexus.comun.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EjecutorEvaluacionesTest {

    private static final CasoEvaluacion CASO = new CasoEvaluacion(
            "c1", "¿Qué contratos vencen?", List.of("contrato", "30 días"), List.of("borré"));

    @Test
    void apruebaRespuestaQueCumpleLasReglas() {
        var informe = new EjecutorEvaluaciones(p -> "Vencen 3 contratos en los próximos 30 días.")
                .ejecutar(List.of(CASO));

        assertThat(informe.aprobados()).isEqualTo(1);
        assertThat(informe.tasaAprobacion()).isEqualTo(1.0);
    }

    @Test
    void reportaFaltantesYProhibidos() {
        var informe = new EjecutorEvaluaciones(p -> "Borré los contratos.").ejecutar(List.of(CASO));

        var resultado = informe.resultados().get(0);
        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.fallos()).containsExactly("falta: 30 días", "contiene prohibido: borré");
    }

    @Test
    void marcaRespuestaConDatosSensibles() {
        var informe = new EjecutorEvaluaciones(p -> "Contrato 30 días, RFC XAXX010101000.")
                .ejecutar(List.of(CASO));

        assertThat(informe.resultados().get(0).fallos())
                .contains("la respuesta contiene datos sensibles");
    }

    @Test
    void cargaLosCasosDelRecurso() {
        List<CasoEvaluacion> casos = CargadorCasos.cargar(getClass().getClassLoader());

        assertThat(casos).hasSize(5);
        assertThat(casos.get(0).id()).isEqualTo("reportes-ventas-mes");
        assertThat(casos.get(0).debeContener()).containsExactly("cliente", "mes");
    }

    @Test
    void lineaBaseSinGeneradorRealFallaTodosLosCasos() {
        // Sin modelo conectado, la respuesta vacía no cumple nada: la línea base queda en 0%.
        var informe = new EjecutorEvaluaciones(p -> "")
                .ejecutar(CargadorCasos.cargar(getClass().getClassLoader()));

        assertThat(informe.total()).isEqualTo(5);
        assertThat(informe.aprobados()).isZero();
    }
}
