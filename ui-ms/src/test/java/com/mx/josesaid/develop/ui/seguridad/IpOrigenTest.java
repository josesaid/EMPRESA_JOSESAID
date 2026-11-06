package com.mx.josesaid.develop.ui.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class IpOrigenTest {

    private static HttpServletRequest solicitud(String directa, String xff) {
        var r = mock(HttpServletRequest.class);
        when(r.getRemoteAddr()).thenReturn(directa);
        when(r.getHeader("X-Forwarded-For")).thenReturn(xff);
        return r;
    }

    @Test
    void sinProxiesConfiablesUsaLaDirecta() {
        var origen = new IpOrigen("");
        assertThat(origen.resolver(solicitud("172.19.0.1", "203.0.113.7"))).isEqualTo("172.19.0.1");
    }

    @Test
    void navegadorNoConfiableNoPuedeFalsearSuIp() {
        var origen = new IpOrigen("10.0.0.0/8");
        assertThat(origen.resolver(solicitud("198.51.100.9", "203.0.113.7"))).isEqualTo("198.51.100.9");
    }

    @Test
    void proxyConfiableEntregaLaIpDelNavegador() {
        var origen = new IpOrigen("10.0.0.0/8");
        assertThat(origen.resolver(solicitud("10.0.0.5", "203.0.113.7"))).isEqualTo("203.0.113.7");
    }

    @Test
    void proxyConfiableConEncabezadoInvalidoUsaLaDirecta() {
        var origen = new IpOrigen("10.0.0.0/8");
        assertThat(origen.resolver(solicitud("10.0.0.5", "basura"))).isEqualTo("10.0.0.5");
    }

    @Test
    void rangoInvalidoFallaAlConfigurar() {
        var origen = new IpOrigen("10.0.0.0/8");
        assertThatThrownBy(() -> IpOrigen.perteneceAlRango("10.0.0.5", "10.0.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
