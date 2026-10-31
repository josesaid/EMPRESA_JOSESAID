package com.mx.josesaid.develop.nexus.evaluacion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;
import com.mx.josesaid.develop.nexus.comun.evaluacion.CargadorCasos;
import com.mx.josesaid.develop.nexus.comun.evaluacion.EjecutorEvaluaciones;

/**
 * Línea base de calidad (paso 22). Llama al modelo real con los casos de nexus-comun.
 * Solo corre si ANTHROPIC_API_KEY está definida; sin ella se omite.
 * Sin servicios de negocio levantados, el asistente responde sin datos: esta medición
 * valida reglas de forma, idioma y redacción. La medición con datos reales se hace con el
 * stack completo levantado (ver README).
 * Escribe el informe en target/linea-base-nexus.txt.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class LineaBaseIT {

    @Autowired
    private AsistenteIA asistente;

    @Test
    void registraLaLineaBase() throws Exception {
        var casos = CargadorCasos.cargar(getClass().getClassLoader());
        var ejecutor = new EjecutorEvaluaciones(
                pregunta -> asistente.consultar(new PreguntaAsistente("evaluacion", pregunta)).texto());

        var informe = ejecutor.ejecutar(casos);

        StringBuilder salida = new StringBuilder();
        salida.append(String.format("Línea base: %d de %d aprobados (%.0f%%)%n",
                informe.aprobados(), informe.total(), informe.tasaAprobacion() * 100));
        informe.resultados().forEach(r -> salida.append(String.format("%s %s %s%n",
                r.aprobado() ? "OK   " : "FALLO", r.id(), r.fallos())));
        Path destino = Path.of("target", "linea-base-nexus.txt");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, salida.toString(), StandardCharsets.UTF_8);
        System.out.println(salida);
    }
}
