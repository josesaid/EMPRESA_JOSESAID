package com.mx.josesaid.develop.nexus.infrastructure;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.mx.josesaid.develop.nexus.application.RecuperadorPoliticas;
import com.mx.josesaid.develop.nexus.application.ResultadoPolitica;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

/**
 * Busca en las políticas internas solo lo que los roles del usuario pueden ver.
 * El resultado trae la fuente de cada fragmento para que la respuesta pueda citarla.
 */
public final class HerramientasPoliticas {

    static final int LIMITE = 5;

    private final RecuperadorPoliticas recuperador;

    public HerramientasPoliticas(RecuperadorPoliticas recuperador) {
        this.recuperador = recuperador;
    }

    @Tool(name = "consultar_politicas",
          description = "Busca en las políticas internas de la empresa que tu rol puede consultar y devuelve los fragmentos relevantes con su fuente. "
                  + "Úsala para preguntas sobre reglas, límites o procesos internos. "
                  + "Cita cada dato con la fuente entre corchetes, por ejemplo [título, regla 2]. "
                  + "No afirmes una política que no aparezca en el resultado.")
    public String consultarPoliticas(
            @ToolParam(description = "Pregunta sobre la política que se quiere consultar", required = true) String pregunta) {
        RegistroHerramientasUsadas.registrar("consultar_politicas");
        List<ResultadoPolitica> resultados = recuperador.buscar(pregunta, AlcanceHerramientas.rolesDelUsuario(), LIMITE);
        if (resultados.isEmpty()) {
            return "No encontré políticas vigentes para tus roles que respondan a esa pregunta.";
        }
        String texto = resultados.stream()
                .map(r -> "[" + r.titulo() + ", regla " + (r.indice() + 1) + "] " + r.texto())
                .collect(joining("\n"));
        return FiltroDatosSensibles.redactar(texto).texto();
    }
}
