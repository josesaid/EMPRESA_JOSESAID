package com.mx.josesaid.develop.nexus.config;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.AlmacenFragmentos;
import com.mx.josesaid.develop.nexus.application.Embedder;
import com.mx.josesaid.develop.nexus.application.IngestaPoliticas;
import com.mx.josesaid.develop.nexus.application.RecuperadorPoliticas;
import com.mx.josesaid.develop.nexus.infrastructure.EjecutorAccionesGateway;
import com.mx.josesaid.develop.nexus.infrastructure.PoliticasClasspath;
import com.mx.josesaid.develop.nexus.infrastructure.VoyageEmbedder;
import com.mx.josesaid.develop.nexus.infrastructure.persistence.AlmacenFragmentosJdbc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.ConsumoTokens;
import com.mx.josesaid.develop.nexus.application.PresupuestoTokens;
import com.mx.josesaid.develop.nexus.comun.prompts.RepositorioPrompts;
import com.mx.josesaid.develop.nexus.comun.tokens.PreciosTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;
import com.mx.josesaid.develop.nexus.infrastructure.ClaudeAsistenteIA;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasClientes;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasCompras;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasFinanzas;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasContabilidad;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasInventario;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasDocumentos;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasUsuarios;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasVentas;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasReportes;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class NexusConfig {

    private static final Logger log = LoggerFactory.getLogger(NexusConfig.class);

    @Bean
    RepositorioPrompts repositorioPrompts() {
        return new RepositorioPrompts();
    }

    @Bean
    Embedder embedder(@Value("${nexus.rag.voyage.url:https://api.voyageai.com}") String url,
                      @Value("${nexus.rag.voyage.api-key:}") String clave,
                      @Value("${nexus.rag.voyage.modelo:voyage-3}") String modelo) {
        return new VoyageEmbedder(RestClient.builder().baseUrl(url).build(), clave, modelo);
    }

    @Bean
    AlmacenFragmentos almacenFragmentos(JdbcTemplate jdbc, PlatformTransactionManager transacciones) {
        return new AlmacenFragmentosJdbc(jdbc, new TransactionTemplate(transacciones));
    }

    @Bean
    IngestaPoliticas ingestaPoliticas(Embedder embedder, AlmacenFragmentos almacen) {
        return new IngestaPoliticas(embedder, almacen);
    }

    @Bean
    RecuperadorPoliticas recuperadorPoliticas(Embedder embedder, AlmacenFragmentos almacen) {
        // Vigencia en hora de México: una política que entra en vigor el 1 de noviembre aplica desde esa fecha local
        return new RecuperadorPoliticas(embedder, almacen, Clock.system(ZoneId.of("America/Mexico_City")));
    }

    /** Indexa las políticas al arrancar. Si falta la clave o Voyage falla, el servicio arranca igual y lo registra. */
    @Bean
    ApplicationRunner ingestaPoliticasAlArrancar(IngestaPoliticas ingesta,
                                                 @Value("${nexus.rag.ingesta-al-arrancar:true}") boolean activa,
                                                 @Value("${nexus.rag.voyage.api-key:}") String clave) {
        return args -> {
            if (!activa) {
                return;
            }
            if (clave.isBlank()) {
                log.warn("Ingesta de políticas omitida: falta VOYAGE_API_KEY");
                return;
            }
            try {
                int fragmentos = ingesta.ingerir(PoliticasClasspath.cargar());
                log.info("Políticas indexadas: {} fragmentos", fragmentos);
            } catch (IOException | RuntimeException e) {
                log.error("Falló la ingesta de políticas; Nexus arranca sin RAG de políticas", e);
            }
        };
    }

    @Bean
    PresupuestoTokens presupuestoTokens(ConsumoTokens consumo, MeterRegistry registro,
                                        @Value("${nexus.presupuesto.tokens-mensuales:150000}") long mensuales,
                                        @Value("${nexus.presupuesto.tokens-diarios:15000}") long diarios,
                                        @Value("${nexus.presupuesto.tokens-servicio-mensuales:1000000}") long servicio) {
        return new PresupuestoTokens(consumo, mensuales, diarios, servicio, Clock.systemUTC(), registro);
    }

    @Bean
    RegistroTokens registroTokens(MeterRegistry registro) {
        // Precios por millón de tokens: se configuran por entorno en un paso posterior.
        return new RegistroTokens(registro, PreciosTokens.SIN_PRECIO);
    }

    @Bean
    HerramientasReportes herramientasReportes(RestClient gatewayClient) {
        return new HerramientasReportes(gatewayClient);
    }

    @Bean
    HerramientasClientes herramientasClientes(RestClient gatewayClient) {
        return new HerramientasClientes(gatewayClient);
    }

    @Bean
    AccionesPendientes accionesPendientes(AlmacenAcciones almacen, RestClient gatewayClient) {
        return new AccionesPendientes(almacen, new EjecutorAccionesGateway(gatewayClient), Clock.systemUTC());
    }

    @Bean
    HerramientasVentas herramientasVentas(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasVentas(gatewayClient, acciones);
    }

    @Bean
    HerramientasCompras herramientasCompras(RestClient gatewayClient) {
        return new HerramientasCompras(gatewayClient);
    }

    @Bean
    HerramientasFinanzas herramientasFinanzas(RestClient gatewayClient) {
        return new HerramientasFinanzas(gatewayClient);
    }

    @Bean
    HerramientasContabilidad herramientasContabilidad(RestClient gatewayClient) {
        return new HerramientasContabilidad(gatewayClient);
    }

    @Bean
    HerramientasInventario herramientasInventario(RestClient gatewayClient) {
        return new HerramientasInventario(gatewayClient);
    }

    @Bean
    HerramientasDocumentos herramientasDocumentos(RestClient gatewayClient) {
        return new HerramientasDocumentos(gatewayClient);
    }

    @Bean
    HerramientasUsuarios herramientasUsuarios(RestClient gatewayClient) {
        return new HerramientasUsuarios(gatewayClient);
    }

    @Bean
    AsistenteIA asistenteIA(ChatModel modelo, RegistroTokens registro, RepositorioPrompts prompts,
                            HerramientasReportes reportes, HerramientasClientes clientes,
                            HerramientasVentas ventas, HerramientasCompras compras,
                            HerramientasFinanzas finanzas, HerramientasContabilidad contabilidad,
                            HerramientasInventario inventario, HerramientasDocumentos documentos,
                            HerramientasUsuarios usuarios) {
        return new ClaudeAsistenteIA(modelo, registro, prompts, reportes, clientes, ventas, compras, finanzas,
                contabilidad, inventario, documentos, usuarios);
    }
}
