package com.mx.josesaid.develop.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class LimiteTamanoCuerpoFilterTest {

    private final LimiteTamanoCuerpoFilter filtro = new LimiteTamanoCuerpoFilter(65536, 21_000_000);

    @Test
    void rechazaCuerpoGeneralMayorA64KiB() {
        var exchange = post("/clientes-ms/v1/clientes", 70_000);
        var llamado = new AtomicBoolean(false);

        filtro.filter(exchange, chain(llamado)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(llamado).isFalse();
    }

    @Test
    void dejaPasarCuerpoGeneralDentroDelLimite() {
        var exchange = post("/clientes-ms/v1/clientes", 1_000);
        var llamado = new AtomicBoolean(false);

        filtro.filter(exchange, chain(llamado)).block();

        assertThat(llamado).isTrue();
    }

    @Test
    void dejaPasarCargaDeDocumentoDeHasta20MB() {
        var exchange = post("/documentos-cumplimiento-ms/v1/documentos", 20_000_000);
        var llamado = new AtomicBoolean(false);

        filtro.filter(exchange, chain(llamado)).block();

        assertThat(llamado).isTrue();
    }

    @Test
    void rechazaCargaDeDocumentoMayorAlLimiteDeDocumentos() {
        var exchange = post("/documentos-cumplimiento-ms/v1/documentos", 22_000_000);
        var llamado = new AtomicBoolean(false);

        filtro.filter(exchange, chain(llamado)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(llamado).isFalse();
    }

    @Test
    void sinContentLengthPasaSinCambios() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/clientes-ms/v1/clientes").build());
        var llamado = new AtomicBoolean(false);

        filtro.filter(exchange, chain(llamado)).block();

        assertThat(llamado).isTrue();
    }

    private static MockServerWebExchange post(String ruta, long tamano) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.post(ruta).header(HttpHeaders.CONTENT_LENGTH, String.valueOf(tamano)).build());
    }

    private static WebFilterChain chain(AtomicBoolean llamado) {
        return exchange -> {
            llamado.set(true);
            return Mono.empty();
        };
    }
}
