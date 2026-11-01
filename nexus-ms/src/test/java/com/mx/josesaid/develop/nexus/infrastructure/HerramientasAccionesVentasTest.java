package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.EjecutorAcciones;

class HerramientasAccionesVentasTest {

    private static final String ID = "7d3c1f2e-0000-4000-8000-000000000002";
    private static final String URL_PEDIDO = "http://gateway/pedidos-ventas-ms/v1/pedidos/" + ID;
    private static final String URL_COTIZACION = "http://gateway/pedidos-ventas-ms/v1/cotizaciones/" + ID;
    private static final String URL_CLIENTE = "http://gateway/clientes-ms/v1/clientes/" + ID;
    private static final String URL_PROSPECTO = "http://gateway/clientes-ms/v1/prospectos/" + ID;

    private final Map<UUID, AccionPendiente> almacen = new HashMap<>();
    private AccionesPendientes acciones;
    private RestClient.Builder builder;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        AlmacenAcciones repo = new AlmacenAcciones() {
            @Override
            public void guardar(AccionPendiente a) {
                almacen.put(a.id(), a);
            }

            @Override
            public Optional<AccionPendiente> buscar(UUID id) {
                return Optional.ofNullable(almacen.get(id));
            }
        };
        EjecutorAcciones ejecutor = (tipo, payload) -> {
            throw new AssertionError("preparar no debe ejecutar la acción");
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
        acciones = new AccionesPendientes(repo, ejecutor, reloj);
        builder = RestClient.builder().baseUrl("http://gateway");
        servidor = MockRestServiceServer.bindTo(builder).build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("u-ventas")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VENTAS"))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private HerramientasVentas herramientas() {
        return new HerramientasVentas(builder.build(), acciones);
    }

    @Test
    void prospectoSePreparaSinLlamarAlGateway() {
        String texto = herramientas().crearProspectoBorrador("Ana Prospecto", "ana@ejemplo.com", "", "referido");

        assertThat(texto).contains("no guardado").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PROSPECTO_BORRADOR);
            assertThat(a.payload()).contains("\"fuente\":\"REFERIDO\"");
        });
        servidor.verify();
    }

    @Test
    void prospectoRechazaUnaFuenteDesconocida() {
        String texto = herramientas().crearProspectoBorrador("Ana", "ana@ejemplo.com", "", "radio");

        assertThat(texto).contains("CAMPANA, REFERIDO o EVENTO");
        assertThat(almacen).isEmpty();
    }

    @Test
    void confirmarPedidoSePreparaSiEstaEnBorrador() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"BORRADOR\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().confirmarPedido(ID);

        assertThat(texto).contains("no ejecutada").contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_CONFIRMAR));
        servidor.verify();
    }

    @Test
    void confirmarPedidoRechazaUnPedidoYaConfirmado() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().confirmarPedido(ID);

        assertThat(texto).contains("solo un pedido en borrador puede confirmarse");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void confirmarPedidoInexistenteResponde() {
        servidor.expect(requestTo(URL_PEDIDO)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        String texto = herramientas().confirmarPedido(ID);

        assertThat(texto).isEqualTo("No encontré ese pedido.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelarPedidoRechazaLaDevolucionTotal() {
        String texto = herramientas().cancelarPedido(ID, "DEVOLUCION_TOTAL");

        assertThat(texto).contains("CANCELACION_CLIENTE o ERROR_CAPTURA");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cancelarPedidoSePreparaSiEstaConfirmado() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().cancelarPedido(ID, "error_captura");

        assertThat(texto).contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_CANCELAR);
            assertThat(a.payload()).contains("\"motivo\":\"ERROR_CAPTURA\"");
        });
        servidor.verify();
    }

    @Test
    void enviarCotizacionSePreparaSiEstaEnBorrador() {
        servidor.expect(requestTo(URL_COTIZACION))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"BORRADOR\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().enviarCotizacion(ID);

        assertThat(texto).contains("no ejecutado").contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_COTIZACION_ENVIAR));
        servidor.verify();
    }

    @Test
    void aceptarCotizacionRechazaUnaCotizacionEnBorrador() {
        servidor.expect(requestTo(URL_COTIZACION))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"BORRADOR\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().aceptarCotizacion(ID);

        assertThat(texto).contains("solo una cotización enviada puede aceptarse");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void convertirCotizacionAceptadaSePreparaComoPedido() {
        servidor.expect(requestTo(URL_COTIZACION))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"ACEPTADA\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().convertirCotizacionEnPedido(ID);

        assertThat(texto).contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_COTIZACION_A_PEDIDO));
        servidor.verify();
    }

    @Test
    void crearCotizacionSePreparaSiElClienteExiste() {
        servidor.expect(requestTo(URL_CLIENTE))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().crearCotizacionBorrador(ID, "web", "mxn", "iva_general", "2026-12-31",
                "prod-1", "", 2, "100.00");

        assertThat(texto).contains("no guardada").contains("accion_id");
        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_COTIZACION_CREAR);
            assertThat(a.payload()).contains("\"vigenteHasta\":\"2026-12-31\"")
                    .contains("\"tasaIva\":\"IVA_GENERAL\"").contains("\"canal\":\"WEB\"");
        });
        servidor.verify();
    }

    @Test
    void crearCotizacionRechazaUnaVigenciaInvalidaSinConsultarNada() {
        String texto = herramientas().crearCotizacionBorrador(ID, "WEB", "MXN", "IVA_GENERAL", "31/12/2026",
                "prod-1", "", 2, "100.00");

        assertThat(texto).contains("formato AAAA-MM-DD");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void crearCotizacionRechazaUnClienteInexistente() {
        servidor.expect(requestTo(URL_CLIENTE)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        String texto = herramientas().crearCotizacionBorrador(ID, "WEB", "MXN", "IVA_GENERAL", "2026-12-31",
                "prod-1", "", 2, "100.00");

        assertThat(texto).contains("No encontré ese cliente.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void reemplazarLineasSoloSePreparaEnBorrador() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().reemplazarLineasPedido(ID, "prod-1", "", 3, "50");

        assertThat(texto).contains("solo un pedido en borrador admite cambios en sus líneas");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void reemplazarLineasPreparaLaAccionEnBorrador() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"BORRADOR\"}", MediaType.APPLICATION_JSON));

        herramientas().reemplazarLineasPedido(ID, "prod-1", "Tornillo", 3, "50");

        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_REEMPLAZAR_LINEAS);
            assertThat(a.payload()).contains("\"pedidoId\":\"" + ID + "\"").contains("\"cantidad\":3");
        });
        servidor.verify();
    }

    @Test
    void devolucionSoloDePedidoEntregado() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONFIRMADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().registrarDevolucionPedido(ID);

        assertThat(texto).contains("solo un pedido entregado puede devolverse");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void devolucionDePedidoEntregadoSePreparaComoAccion() {
        servidor.expect(requestTo(URL_PEDIDO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"ENTREGADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().registrarDevolucionPedido(ID);

        assertThat(texto).contains("no registrada").contains("accion_id");
        assertThat(almacen.values()).singleElement()
                .satisfies(a -> assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PEDIDO_DEVOLVER));
        servidor.verify();
    }

    @Test
    void cambiarEstatusNoAceptaConvertidoNiConsultaNada() {
        String texto = herramientas().cambiarEstatusProspecto(ID, "CONVERTIDO");

        assertThat(texto).contains("convertir_prospecto_en_cliente");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cambiarEstatusRespetaLasTransiciones() {
        servidor.expect(requestTo(URL_PROSPECTO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"NUEVO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().cambiarEstatusProspecto(ID, "CALIFICADO");

        assertThat(texto).contains("No se puede pasar el prospecto de NUEVO a CALIFICADO.");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void cambiarEstatusValidoSePreparaComoAccion() {
        servidor.expect(requestTo(URL_PROSPECTO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONTACTADO\"}", MediaType.APPLICATION_JSON));

        herramientas().cambiarEstatusProspecto(ID, "calificado");

        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PROSPECTO_ESTATUS);
            assertThat(a.payload()).contains("\"estatus\":\"CALIFICADO\"");
        });
        servidor.verify();
    }

    @Test
    void convertirProspectoSoloSiEstaCalificado() {
        servidor.expect(requestTo(URL_PROSPECTO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CONTACTADO\"}", MediaType.APPLICATION_JSON));

        String texto = herramientas().convertirProspectoEnCliente(ID, "Acme SA", "ACM010101AB1", "Ana",
                "ana@acme.mx", "COMPRAS");

        assertThat(texto).contains("solo un prospecto calificado puede convertirse en cliente");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void convertirProspectoCalificadoSePreparaConSuContacto() {
        servidor.expect(requestTo(URL_PROSPECTO))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"estatus\":\"CALIFICADO\"}", MediaType.APPLICATION_JSON));

        herramientas().convertirProspectoEnCliente(ID, "Acme SA", "ACM010101AB1", "Ana", "ana@acme.mx", "compras");

        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_PROSPECTO_CONVERTIR);
            assertThat(a.payload()).contains("\"contactoPrincipal\"").contains("\"rol\":\"COMPRAS\"");
        });
        servidor.verify();
    }

    @Test
    void interaccionRechazaTipoYFechaInvalidosSinConsultarNada() {
        HerramientasVentas herramientas = herramientas();

        assertThat(herramientas.registrarInteraccionCliente(ID, "CHAT", "2026-11-01T10:00:00-06:00", "Hola"))
                .contains("LLAMADA, CORREO o VISITA");
        assertThat(herramientas.registrarInteraccionCliente(ID, "LLAMADA", "ayer", "Hola"))
                .contains("formato ISO 8601");
        assertThat(almacen).isEmpty();
        servidor.verify();
    }

    @Test
    void interaccionSePreparaConLaFechaEnInstante() {
        servidor.expect(requestTo(URL_CLIENTE))
                .andRespond(withSuccess("{\"id\":\"" + ID + "\"}", MediaType.APPLICATION_JSON));

        herramientas().registrarInteraccionCliente(ID, "llamada", "2026-11-01T10:00:00-06:00", "Seguimiento");

        assertThat(almacen.values()).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo(AccionesPendientes.TIPO_INTERACCION_REGISTRAR);
            assertThat(a.payload()).contains("\"fecha\":\"2026-11-01T16:00:00Z\"").contains("\"tipo\":\"LLAMADA\"");
        });
        servidor.verify();
    }
}
