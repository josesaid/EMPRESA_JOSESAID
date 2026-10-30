package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Ejecuta los casos contra un generador de respuestas (el modelo en producción, un doble en pruebas)
 * y calcula la línea base de calidad con reglas deterministas.
 */
public final class EjecutorEvaluaciones {

    public record Informe(int total, int aprobados, List<ResultadoCaso> resultados) {

        public double tasaAprobacion() {
            return total == 0 ? 0.0 : (double) aprobados / total;
        }
    }

    private final Function<String, String> generador;

    public EjecutorEvaluaciones(Function<String, String> generador) {
        this.generador = generador;
    }

    public Informe ejecutar(List<CasoEvaluacion> casos) {
        List<ResultadoCaso> resultados = new ArrayList<>();
        int aprobados = 0;
        for (CasoEvaluacion caso : casos) {
            String respuesta = generador.apply(caso.pregunta());
            List<String> fallos = evaluar(caso, respuesta);
            boolean ok = fallos.isEmpty();
            if (ok) {
                aprobados++;
            }
            resultados.add(new ResultadoCaso(caso.id(), ok, fallos, respuesta));
        }
        return new Informe(casos.size(), aprobados, resultados);
    }

    static List<String> evaluar(CasoEvaluacion caso, String respuesta) {
        List<String> fallos = new ArrayList<>();
        String minusculas = respuesta == null ? "" : respuesta.toLowerCase(Locale.ROOT);
        for (String esperado : caso.debeContener()) {
            if (!minusculas.contains(esperado.toLowerCase(Locale.ROOT))) {
                fallos.add("falta: " + esperado);
            }
        }
        for (String prohibido : caso.noDebeContener()) {
            if (minusculas.contains(prohibido.toLowerCase(Locale.ROOT))) {
                fallos.add("contiene prohibido: " + prohibido);
            }
        }
        if (respuesta != null && FiltroDatosSensibles.redactar(respuesta).total() > 0) {
            fallos.add("la respuesta contiene datos sensibles");
        }
        return fallos;
    }
}
