package com.mx.josesaid.develop.nexus.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.nexus.application.ContextoContable.CuentaCatalogo;
import com.mx.josesaid.develop.nexus.application.ContextoContable.PolizaEjemplo;
import com.mx.josesaid.develop.nexus.application.PolizaSugerida;
import com.mx.josesaid.develop.nexus.application.PolizaSugerida.LineaSugerida;
import com.mx.josesaid.develop.nexus.application.SugeridorPolizas;
import com.mx.josesaid.develop.nexus.application.SugerenciaPolizaInvalidaException;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensiblesAdvisor;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokensAdvisor;

/**
 * Sugiere pólizas con Claude usando pólizas históricas como ejemplos (few-shot).
 * El modelo solo devuelve JSON: no tiene herramientas para registrar nada.
 */
public final class ClaudeSugeridorPolizas implements SugeridorPolizas {

    static final String SERVICIO = "nexus-ms";

    static final String INSTRUCCIONES = """
            Eres un asistente contable. Propones una póliza a partir de la descripción del usuario.
            Usa solo cuentas del catálogo, identificadas por su código.
            Responde únicamente con JSON, sin texto adicional, con esta forma:
            {"tipo":"INGRESO|EGRESO|DIARIO","lineas":[{"codigo":"...","cargo":0,"abono":0}],"confianza":0.0,"motivo":"..."}
            Cada línea tiene cargo o abono, y el otro en cero. La suma de cargos debe ser igual a la de abonos.
            Si no estás seguro, baja la confianza. Los ejemplos son pólizas históricas de referencia.
            """;

    private final ChatClient cliente;
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ClaudeSugeridorPolizas(ChatModel modelo, RegistroTokens registro) {
        this.cliente = ChatClient.builder(modelo)
                .defaultSystem(INSTRUCCIONES)
                .defaultAdvisors(new FiltroDatosSensiblesAdvisor(), new RegistroTokensAdvisor(registro))
                .build();
    }

    @Override
    public PolizaSugerida sugerir(String usuario, String descripcion, List<CuentaCatalogo> catalogo,
                                  List<PolizaEjemplo> ejemplos) {
        String respuesta = cliente.prompt()
                .user(contexto(descripcion, catalogo, ejemplos))
                .advisors(a -> a
                        .param(RegistroTokensAdvisor.CLAVE_SERVICIO, SERVICIO)
                        .param(RegistroTokensAdvisor.CLAVE_USUARIO, usuario))
                .call()
                .content();
        RespuestaModelo r = parsear(respuesta);
        List<LineaSugerida> lineas = r.lineas() == null ? List.of() : r.lineas().stream()
                .map(l -> new LineaSugerida(l.codigo(), l.cargo(), l.abono()))
                .toList();
        return new PolizaSugerida(r.tipo(), lineas, r.confianza(), r.motivo());
    }

    static String contexto(String descripcion, List<CuentaCatalogo> catalogo, List<PolizaEjemplo> ejemplos) {
        Map<java.util.UUID, String> codigoPorId = catalogo.stream()
                .collect(Collectors.toMap(CuentaCatalogo::id, CuentaCatalogo::codigo, (a, b) -> a));
        StringBuilder sb = new StringBuilder("Catálogo de cuentas:\n");
        catalogo.forEach(c -> sb.append(c.codigo()).append(' ').append(c.nombre()).append('\n'));
        sb.append("\nPólizas históricas de referencia:\n");
        for (PolizaEjemplo e : ejemplos) {
            sb.append(e.tipo()).append(' ').append(e.fecha()).append(": ");
            sb.append(e.lineas().stream()
                    .map(l -> codigoPorId.getOrDefault(l.cuentaId(), "?") + " "
                            + (l.cargo().signum() > 0 ? "cargo " + l.cargo() : "abono " + l.abono()))
                    .collect(Collectors.joining("; ")));
            sb.append('\n');
        }
        sb.append("\nDescripción de la operación: ").append(descripcion);
        return sb.toString();
    }

    private RespuestaModelo parsear(String respuesta) {
        if (respuesta == null) {
            throw new SugerenciaPolizaInvalidaException("El modelo no devolvió respuesta");
        }
        int inicio = respuesta.indexOf('{');
        int fin = respuesta.lastIndexOf('}');
        if (inicio < 0 || fin < inicio) {
            throw new SugerenciaPolizaInvalidaException("El modelo no devolvió JSON");
        }
        try {
            return json.readValue(respuesta.substring(inicio, fin + 1), RespuestaModelo.class);
        } catch (JsonProcessingException e) {
            throw new SugerenciaPolizaInvalidaException("El JSON del modelo no es válido");
        }
    }

    record RespuestaModelo(String tipo, List<LineaModelo> lineas, double confianza, String motivo) {
    }

    record LineaModelo(String codigo, BigDecimal cargo, BigDecimal abono) {
    }
}
