package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.Arrays;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;
import com.mx.josesaid.develop.nexus.application.RespuestaAsistente;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensiblesAdvisor;
import com.mx.josesaid.develop.nexus.comun.prompts.PromptVersionado;
import com.mx.josesaid.develop.nexus.comun.prompts.RepositorioPrompts;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokensAdvisor;

/**
 * Adaptador del puerto AsistenteIA sobre Claude, con ChatClient de Spring AI.
 * Los datos sensibles se redactan antes de llamar al modelo y el consumo se mide por llamada.
 */
public final class ClaudeAsistenteIA implements AsistenteIA {

    static final String SERVICIO = "nexus-ms";
    static final String PROMPT = "asistente-base";

    private final ChatClient cliente;
    private final HerramientasReportes reportes;
    private final HerramientasClientes clientes;
    private final HerramientasVentas ventas;
    private final HerramientasCompras compras;
    private final HerramientasFinanzas finanzas;
    private final HerramientasContabilidad contabilidad;
    private final HerramientasInventario inventario;
    private final HerramientasDocumentos documentos;
    private final HerramientasUsuarios usuarios;
    private final ToolCallback[] herramientas;

    public ClaudeAsistenteIA(ChatModel modelo, RegistroTokens registro, RepositorioPrompts prompts,
                             HerramientasReportes reportes, HerramientasClientes clientes,
                             HerramientasVentas ventas, HerramientasCompras compras,
                             HerramientasFinanzas finanzas, HerramientasContabilidad contabilidad,
                             HerramientasInventario inventario, HerramientasDocumentos documentos,
                             HerramientasUsuarios usuarios) {
        this.reportes = reportes;
        this.clientes = clientes;
        this.ventas = ventas;
        this.compras = compras;
        this.finanzas = finanzas;
        this.contabilidad = contabilidad;
        this.inventario = inventario;
        this.documentos = documentos;
        this.usuarios = usuarios;
        this.herramientas = ToolCallbacks.from(reportes, clientes, ventas, compras, finanzas, contabilidad,
                inventario, documentos, usuarios);
        PromptVersionado prompt = prompts.obtener(PROMPT);
        this.cliente = ChatClient.builder(modelo)
                .defaultSystem(prompt.texto())
                .defaultAdvisors(
                        new FiltroDatosSensiblesAdvisor(),
                        new RegistroTokensAdvisor(registro))
                .build();
    }

    @Override
    public RespuestaAsistente consultar(PreguntaAsistente pregunta) {
        var solicitud = cliente.prompt()
                .user(pregunta.texto())
                .advisors(a -> a
                        .param(RegistroTokensAdvisor.CLAVE_SERVICIO, SERVICIO)
                        .param(RegistroTokensAdvisor.CLAVE_USUARIO, pregunta.usuario()));
        ToolCallback[] permitidas = Arrays.stream(herramientas)
                .filter(h -> AlcanceHerramientas.puedeUsar(h.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
        if (permitidas.length > 0) {
            solicitud = solicitud.toolCallbacks(permitidas);
        }

        RegistroHerramientasUsadas.iniciar();
        try {
            ChatResponse respuesta = solicitud.call().chatResponse();
            String texto = respuesta.getResult() == null ? "" : respuesta.getResult().getOutput().getText();
            var metadatos = respuesta.getMetadata();
            var uso = metadatos == null ? null : metadatos.getUsage();
            Integer entrada = uso == null ? null : uso.getPromptTokens();
            Integer salida = uso == null ? null : uso.getCompletionTokens();
            return new RespuestaAsistente(texto, entrada, salida, RegistroHerramientasUsadas.terminar());
        } finally {
            RegistroHerramientasUsadas.terminar();
        }
    }
}
