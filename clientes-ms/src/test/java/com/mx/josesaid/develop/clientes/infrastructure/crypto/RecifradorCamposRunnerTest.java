package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;

class RecifradorCamposRunnerTest {

    private final SecureRandom aleatorio = new SecureRandom();
    private final CampoCifradoConverter convertidor = new CampoCifradoConverter();
    private byte[] clave1;
    private byte[] clave2;
    private RecifradorJdbc almacen;
    private RecifradorCamposRunner runner;

    @BeforeEach
    void preparar() {
        clave1 = bytes();
        clave2 = bytes();
        ClavesCripto.configurar(Map.of(1, clave1), 1, bytes());
        almacen = mock(RecifradorJdbc.class);
        runner = new RecifradorCamposRunner(almacen);
    }

    @Test
    void sinFilasPendientesNoActualizaNada() {
        when(almacen.pendientes(anyString(), anyString(), anyInt())).thenReturn(List.of());

        runner.run(mock(ApplicationArguments.class));

        verify(almacen, times(7)).pendientes(anyString(), anyString(), eq(500));
        verify(almacen, never()).actualizar(anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void recifraLaFilaViejaConLaVersionActiva() {
        String viejo = convertidor.convertToDatabaseColumn("ana@empresa.mx");
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytes());

        when(almacen.pendientes(anyString(), anyString(), anyInt())).thenReturn(List.of());
        when(almacen.pendientes(eq("contacto"), eq("correo_cifrado"), anyInt()))
                .thenReturn(List.of(new RecifradorPorLotes.Fila(1L, viejo)))
                .thenReturn(List.of());
        when(almacen.actualizar(eq("contacto"), eq("correo_cifrado"), eq(1L), eq(viejo), anyString()))
                .thenReturn(true);

        runner.run(mock(ApplicationArguments.class));

        ArgumentCaptor<String> nuevo = ArgumentCaptor.forClass(String.class);
        verify(almacen).actualizar(eq("contacto"), eq("correo_cifrado"), eq(1L), eq(viejo), nuevo.capture());
        assertThat(nuevo.getValue()).startsWith("v2:");
        assertThat(convertidor.convertToEntityAttribute(nuevo.getValue())).isEqualTo("ana@empresa.mx");
    }

    private byte[] bytes() {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        return b;
    }
}
