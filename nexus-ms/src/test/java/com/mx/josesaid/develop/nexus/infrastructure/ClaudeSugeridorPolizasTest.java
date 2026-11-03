package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import com.mx.josesaid.develop.nexus.application.ContextoContable.CuentaCatalogo;
import com.mx.josesaid.develop.nexus.application.ContextoContable.LineaEjemplo;
import com.mx.josesaid.develop.nexus.application.ContextoContable.PolizaEjemplo;
import com.mx.josesaid.develop.nexus.application.PolizaSugerida;
import com.mx.josesaid.develop.nexus.application.SugerenciaPolizaInvalidaException;
import com.mx.josesaid.develop.nexus.comun.tokens.PreciosTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ClaudeSugeridorPolizasTest {

    private static final UUID CAJA = UUID.fromString("00000000-0000-4000-8000-000000000101");
    private static final UUID VENTAS = UUID.fromString("00000000-0000-4000-8000-000000000401");
    private static final List<CuentaCatalogo> CATALOGO = List.of(
            new CuentaCatalogo(CAJA, "101", "Caja"),
            new CuentaCatalogo(VENTAS, "401", "Ventas"));

    private static ClaudeSugeridorPolizas sugeridor(String respuestaDelModelo) {
        ChatModel modelo = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(respuestaDelModelo))));
            }
        };
        return new ClaudeSugeridorPolizas(modelo,
                new RegistroTokens(new SimpleMeterRegistry(), PreciosTokens.SIN_PRECIO));
    }

    @Test
    void leeLaSugerenciaAunqueVengaEnBloqueDeCodigo() {
        String respuesta = """
                ```json
                {"tipo":"INGRESO","lineas":[{"codigo":"101","cargo":1000,"abono":0},
                {"codigo":"401","cargo":0,"abono":1000}],"confianza":0.9,"motivo":"Venta de contado"}
                ```
                """;

        PolizaSugerida s = sugeridor(respuesta).sugerir("contador1", "venta", CATALOGO, List.of());

        assertThat(s.tipo()).isEqualTo("INGRESO");
        assertThat(s.confianza()).isEqualTo(0.9);
        assertThat(s.lineas()).hasSize(2);
        assertThat(s.lineas().get(0).codigoCuenta()).isEqualTo("101");
        assertThat(s.lineas().get(0).cargo()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void unaRespuestaSinJsonSeRechaza() {
        assertThatThrownBy(() -> sugeridor("No sé contestar").sugerir("contador1", "venta", CATALOGO, List.of()))
                .isInstanceOf(SugerenciaPolizaInvalidaException.class);
    }

    @Test
    void elContextoIncluyeCatalogoYEjemplosConCodigos() {
        List<PolizaEjemplo> ejemplos = List.of(new PolizaEjemplo("INGRESO", LocalDate.of(2026, 10, 3),
                List.of(new LineaEjemplo(CAJA, new BigDecimal("500"), BigDecimal.ZERO),
                        new LineaEjemplo(VENTAS, BigDecimal.ZERO, new BigDecimal("500")))));

        String contexto = ClaudeSugeridorPolizas.contexto("Venta de contado", CATALOGO, ejemplos);

        assertThat(contexto).contains("101 Caja").contains("401 Ventas");
        assertThat(contexto).contains("INGRESO 2026-10-03: 101 cargo 500; 401 abono 500");
        assertThat(contexto).contains("Descripción de la operación: Venta de contado");
    }
}
