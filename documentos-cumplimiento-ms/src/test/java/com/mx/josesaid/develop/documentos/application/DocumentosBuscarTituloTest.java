package com.mx.josesaid.develop.documentos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.documentos.domain.Documento;
import com.mx.josesaid.develop.documentos.domain.DocumentoRepositorio;
import com.mx.josesaid.develop.documentos.domain.SolicitudInvalidaException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentosBuscarTituloTest {

    @Test
    void textoCortoSeRechazaSinConsultar() {
        DocumentoRepositorio documentos = mock(DocumentoRepositorio.class);
        DocumentosService servicio = servicio(documentos);

        assertThatThrownBy(() -> servicio.buscarPorTitulo("ab"))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(documentos, never()).buscarPorTitulo(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void recortaElTextoYPideHastaQuince() {
        DocumentoRepositorio documentos = mock(DocumentoRepositorio.class);
        Documento doc = mock(Documento.class);
        when(documentos.buscarPorTitulo(eq("contrato"), eq(15))).thenReturn(List.of(doc));

        assertThat(servicio(documentos).buscarPorTitulo("  contrato ")).containsExactly(doc);
    }

    private static DocumentosService servicio(DocumentoRepositorio documentos) {
        return new DocumentosService(documentos, mock(AlmacenDocumentos.class), mock(PublicadorEventos.class),
                Clock.systemUTC(), new SimpleMeterRegistry(), 30);
    }
}
