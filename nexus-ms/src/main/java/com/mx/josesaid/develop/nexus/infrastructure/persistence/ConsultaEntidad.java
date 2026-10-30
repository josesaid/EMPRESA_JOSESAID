package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nexus_consultas")
public class ConsultaEntidad {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(name = "pregunta_redactada", nullable = false, columnDefinition = "TEXT")
    private String preguntaRedactada;

    @Column(name = "respuesta_resumen", length = 500)
    private String respuestaResumen;

    @Column(length = 500)
    private String herramientas;

    @Column(name = "tokens_entrada")
    private Integer tokensEntrada;

    @Column(name = "tokens_salida")
    private Integer tokensSalida;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    protected ConsultaEntidad() {
    }

    public ConsultaEntidad(UUID id, String usuario, String preguntaRedactada, String respuestaResumen,
                           String herramientas, Integer tokensEntrada, Integer tokensSalida, Instant creadoUtc) {
        this.id = id;
        this.usuario = usuario;
        this.preguntaRedactada = preguntaRedactada;
        this.respuestaResumen = respuestaResumen;
        this.herramientas = herramientas;
        this.tokensEntrada = tokensEntrada;
        this.tokensSalida = tokensSalida;
        this.creadoUtc = creadoUtc;
    }
}
