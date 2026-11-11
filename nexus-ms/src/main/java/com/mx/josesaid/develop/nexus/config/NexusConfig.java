package com.mx.josesaid.develop.nexus.config;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;
import com.mx.josesaid.develop.nexus.application.AlmacenFragmentos;
import com.mx.josesaid.develop.nexus.application.ContextoContable;
import com.mx.josesaid.develop.nexus.application.ServicioPolizas;
import com.mx.josesaid.develop.nexus.application.SugeridorPolizas;
import com.mx.josesaid.develop.nexus.application.Embedder;
import com.mx.josesaid.develop.nexus.application.IngestaPoliticas;
import com.mx.josesaid.develop.nexus.application.RecuperadorPoliticas;
import com.mx.josesaid.develop.nexus.infrastructure.ClaudeSugeridorPolizas;
import com.mx.josesaid.develop.nexus.infrastructure.ContextoContableGateway;
import com.mx.josesaid.develop.nexus.infrastructure.EjecutorAccionesGateway;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasPoliticas;
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

/** Configuración de Nexus: crea los beans de prompts, RAG, presupuesto, herramientas del asistente y servicio de pólizas. */
@Configuration
public class NexusConfig {

    private static final Logger log = LoggerFactory.getLogger(NexusConfig.class);

    /** Repositorio de prompts versionados, leído de los recursos de nexus-comun. */
    @Bean
    RepositorioPrompts repositorioPrompts() {
        return new RepositorioPrompts();
    }

    /** Cliente de embeddings de Voyage. La URL, la clave y el modelo se configuran por entorno. */
    @Bean
    Embedder embedder(@Value("${nexus.rag.voyage.url:https://api.voyageai.com}") String url,
                      @Value("${nexus.rag.voyage.api-key:}") String clave,
                      @Value("${nexus.rag.voyage.modelo:voyage-3}") String modelo) {
        return new VoyageEmbedder(RestClient.builder().baseUrl(url).build(), clave, modelo);
    }

    /** Almacén de fragmentos de políticas en PostgreSQL. Las escrituras van dentro de una transacción. */
    @Bean
    AlmacenFragmentos almacenFragmentos(JdbcTemplate jdbc, PlatformTransactionManager transacciones) {
        return new AlmacenFragmentosJdbc(jdbc, new TransactionTemplate(transacciones));
    }

    /** Servicio que indexa las políticas para la búsqueda. */
    @Bean
    IngestaPoliticas ingestaPoliticas(Embedder embedder, AlmacenFragmentos almacen) {
        return new IngestaPoliticas(embedder, almacen);
    }

    /** Servicio que busca políticas vigentes. Usa hora de México para que una política aplique desde su fecha local. */
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

    /** Control de presupuesto de tokens. Los límites mensual, diario y del servicio se configuran por entorno. */
    @Bean
    PresupuestoTokens presupuestoTokens(ConsumoTokens consumo, MeterRegistry registro,
                                        @Value("${nexus.presupuesto.tokens-mensuales:150000}") long mensuales,
                                        @Value("${nexus.presupuesto.tokens-diarios:15000}") long diarios,
                                        @Value("${nexus.presupuesto.tokens-servicio-mensuales:1000000}") long servicio) {
        return new PresupuestoTokens(consumo, mensuales, diarios, servicio, Clock.systemUTC(), registro);
    }

    /** Registro de consumo de tokens. Los precios aún no están configurados, así que el costo estimado queda en cero. */
    @Bean
    RegistroTokens registroTokens(MeterRegistry registro) {
        // Precios por millón de tokens: se configuran por entorno en un paso posterior.
        return new RegistroTokens(registro, PreciosTokens.SIN_PRECIO);
    }

    /** Herramienta de reportes para el asistente, con acceso al gateway. */
    @Bean
    HerramientasReportes herramientasReportes(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasReportes(gatewayClient, acciones);
    }

    /** Herramienta de clientes para el asistente. */
    @Bean
    HerramientasClientes herramientasClientes(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasClientes(gatewayClient, acciones);
    }

    /** Ejecutor de acciones aprobadas, que las envía al gateway con la hora de UTC. */
    @Bean
    AccionesPendientes accionesPendientes(AlmacenAcciones almacen, RestClient gatewayClient) {
        return new AccionesPendientes(almacen, new EjecutorAccionesGateway(gatewayClient), Clock.systemUTC());
    }

    /** Herramienta de ventas para el asistente. */
    @Bean
    HerramientasVentas herramientasVentas(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasVentas(gatewayClient, acciones);
    }

    /** Herramienta de compras para el asistente. */
    @Bean
    HerramientasCompras herramientasCompras(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasCompras(gatewayClient, acciones);
    }

    /** Herramienta de finanzas para el asistente. */
    @Bean
    HerramientasFinanzas herramientasFinanzas(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasFinanzas(gatewayClient, acciones);
    }

    /** Herramienta de contabilidad. Solo lectura, por eso no recibe el registro de acciones. */
    @Bean
    HerramientasContabilidad herramientasContabilidad(RestClient gatewayClient) {
        return new HerramientasContabilidad(gatewayClient);
    }

    /** Herramienta de inventario para el asistente. */
    @Bean
    HerramientasInventario herramientasInventario(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasInventario(gatewayClient, acciones);
    }

    /** Herramienta de documentos para el asistente. */
    @Bean
    HerramientasDocumentos herramientasDocumentos(RestClient gatewayClient, AccionesPendientes acciones) {
        return new HerramientasDocumentos(gatewayClient, acciones);
    }

    /** Herramienta de usuarios. Recibe el generador de códigos porque las altas usan confirmación. */
    @Bean
    HerramientasUsuarios herramientasUsuarios(RestClient gatewayClient, AccionesPendientes acciones,
                                              CodigoConfirmacion codigos) {
        return new HerramientasUsuarios(gatewayClient, acciones, codigos);
    }

    /** Asistente principal con todas sus herramientas y el modelo de Claude. */
    @Bean
    AsistenteIA asistenteIA(ChatModel modelo, RegistroTokens registro, RepositorioPrompts prompts,
                            HerramientasReportes reportes, HerramientasClientes clientes,
                            HerramientasVentas ventas, HerramientasCompras compras,
                            HerramientasFinanzas finanzas, HerramientasContabilidad contabilidad,
                            HerramientasInventario inventario, HerramientasDocumentos documentos,
                            HerramientasUsuarios usuarios, HerramientasPoliticas politicas) {
        return new ClaudeAsistenteIA(modelo, registro, prompts, reportes, clientes, ventas, compras, finanzas,
                contabilidad, inventario, documentos, usuarios, politicas);
    }

    /** Herramienta que consulta las políticas de la empresa. */
    @Bean
    HerramientasPoliticas herramientasPoliticas(RecuperadorPoliticas recuperador) {
        return new HerramientasPoliticas(recuperador);
    }

    /** Contexto contable (catálogo y ejemplos) leído del gateway, con la zona de México. */
    @Bean
    ContextoContable contextoContable(RestClient gatewayClient) {
        return new ContextoContableGateway(gatewayClient, ZoneId.of("America/Mexico_City"));
    }

    /** Sugeridor de pólizas basado en el modelo de Claude. */
    @Bean
    SugeridorPolizas sugeridorPolizas(ChatModel modelo, RegistroTokens registro) {
        return new ClaudeSugeridorPolizas(modelo, registro);
    }

    /** Servicio de pólizas con el reloj de México. */
    @Bean
    ServicioPolizas servicioPolizas(ContextoContable contexto, SugeridorPolizas sugeridor,
                                    AccionesPendientes acciones, PresupuestoTokens presupuesto) {
        return new ServicioPolizas(contexto, sugeridor, acciones, presupuesto,
                Clock.system(ZoneId.of("America/Mexico_City")));
    }
}
