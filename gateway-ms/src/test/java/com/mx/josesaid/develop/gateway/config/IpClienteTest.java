package com.mx.josesaid.develop.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IpClienteTest {

    private static final List<String> PROXY_UI = List.of("172.19.0.0/16");

    @Test
    void sinProxiesConfiablesUsaLaIpDirecta() {
        assertThat(IpCliente.resolver("172.19.0.5", "203.0.113.7", List.of())).isEqualTo("172.19.0.5");
    }

    @Test
    void conexionNoConfiableIgnoraXForwardedFor() {
        assertThat(IpCliente.resolver("198.51.100.9", "203.0.113.7", PROXY_UI)).isEqualTo("198.51.100.9");
    }

    @Test
    void proxyConfiableUsaElClienteDelEncabezado() {
        assertThat(IpCliente.resolver("172.19.0.5", "203.0.113.7", PROXY_UI)).isEqualTo("203.0.113.7");
    }

    @Test
    void proxyConfiableSinEncabezadoUsaLaIpDirecta() {
        assertThat(IpCliente.resolver("172.19.0.5", null, PROXY_UI)).isEqualTo("172.19.0.5");
    }

    @Test
    void encabezadoInvalidoCaeALaIpDirecta() {
        assertThat(IpCliente.resolver("172.19.0.5", "no-es-ip", PROXY_UI)).isEqualTo("172.19.0.5");
    }

    @Test
    void variosValoresTomaElPrimero() {
        assertThat(IpCliente.resolver("172.19.0.5", "203.0.113.7, 10.0.0.1", PROXY_UI)).isEqualTo("203.0.113.7");
    }

    @Test
    void ipv6ValidaEnEncabezado() {
        assertThat(IpCliente.resolver("172.19.0.5", "2001:db8::1", PROXY_UI)).isEqualTo("2001:db8::1");
    }

    @Test
    void rangoCidrIpv4() {
        assertThat(IpCliente.perteneceAlRango("172.19.255.1", "172.19.0.0/16")).isTrue();
        assertThat(IpCliente.perteneceAlRango("172.20.0.1", "172.19.0.0/16")).isFalse();
    }

    @Test
    void rangoCidrConPrefijoNoAlineado() {
        assertThat(IpCliente.perteneceAlRango("10.0.0.200", "10.0.0.128/25")).isTrue();
        assertThat(IpCliente.perteneceAlRango("10.0.0.5", "10.0.0.128/25")).isFalse();
    }

    @Test
    void rangoInvalidoLanzaError() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> IpCliente.perteneceAlRango("172.19.0.5", "172.19.0.0"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> IpCliente.perteneceAlRango("172.19.0.5", "172.19.0.0/40"));
    }
}
