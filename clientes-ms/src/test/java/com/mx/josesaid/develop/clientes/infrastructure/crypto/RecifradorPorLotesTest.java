package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecifradorPorLotesTest {

    private final CampoCifradoConverter convertidor = new CampoCifradoConverter();
    private final SecureRandom aleatorio = new SecureRandom();
    private byte[] clave1;
    private byte[] clave2;

    @BeforeEach
    void claves() {
        clave1 = bytes();
        clave2 = bytes();
        ClavesCripto.configurar(Map.of(1, clave1), 1, bytes());
    }

    @Test
    void recifraTodasLasFilasPendientesYNoQuedaNinguna() {
        String viejo1 = convertidor.convertToDatabaseColumn("ana@empresa.mx");
        String viejo2 = convertidor.convertToDatabaseColumn("luis@empresa.mx");
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytes());
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        almacen.agregar(UUID.randomUUID(), viejo1);
        almacen.agregar(UUID.randomUUID(), viejo2);

        int recifradas = recifrador(almacen, 10).ejecutar("contacto", "correo_cifrado");

        assertThat(recifradas).isEqualTo(2);
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(v).startsWith("v2:"));
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(convertidor.convertToEntityAttribute(v))
                .isIn("ana@empresa.mx", "luis@empresa.mx"));
    }

    @Test
    void respetaElTamanoDeLote() {
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        for (int i = 0; i < 5; i++) {
            almacen.agregar(UUID.randomUUID(), convertidor.convertToDatabaseColumn("dato" + i));
        }
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytes());

        recifrador(almacen, 2).ejecutar("cliente", "rfc_cifrado");

        assertThat(almacen.limitesPedidos).containsOnly(2);
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(v).startsWith("v2:"));
    }

    @Test
    void omiteLasFilasYaEnLaVersionActiva() {
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytes());
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        String yaNueva = convertidor.convertToDatabaseColumn("ya nueva");
        almacen.agregar(UUID.randomUUID(), yaNueva);

        int recifradas = recifrador(almacen, 10).ejecutar("cliente", "rfc_cifrado");

        assertThat(recifradas).isZero();
        assertThat(almacen.valores()).containsExactly(yaNueva);
    }

    @Test
    void reanudarDespuesDeUnCorteNoDuplicaNiPierde() {
        String viejo1 = convertidor.convertToDatabaseColumn("uno");
        String viejo2 = convertidor.convertToDatabaseColumn("dos");
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytes());
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        almacen.agregar(UUID.randomUUID(), viejo1);
        almacen.agregar(UUID.randomUUID(), viejo2);
        almacen.fallarEnActualizacion = 2;

        assertThatThrownBy(() -> recifrador(almacen, 1).ejecutar("interaccion", "resumen_cifrado"))
                .isInstanceOf(IllegalStateException.class);
        almacen.fallarEnActualizacion = 0;
        int segundaCorrida = recifrador(almacen, 1).ejecutar("interaccion", "resumen_cifrado");

        assertThat(segundaCorrida).isEqualTo(1);
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(v).startsWith("v2:"));
    }

    @Test
    void siUnaFilaCambiaDuranteElProcesoSeAvisa() {
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        almacen.agregar(UUID.randomUUID(), convertidor.convertToDatabaseColumn("x"));
        ClavesCripto.configurar(Map.of(1, clave1, 2, clave2), 2, bytes());
        almacen.rechazarActualizacion = true;

        assertThatThrownBy(() -> recifrador(almacen, 10).ejecutar("contacto", "nombre_cifrado"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cambió durante el proceso");
    }

    @Test
    void tamanoDeLoteDebeSerPositivo() {
        assertThatThrownBy(() -> new RecifradorPorLotes(new AlmacenEnMemoria(), 0, v -> v))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void versionDeIdentificaPrefijoOSinPrefijo() {
        assertThat(CampoCifradoConverter.versionDe("v7:AAAA")).isEqualTo(7);
        assertThat(CampoCifradoConverter.versionDe("AAAA")).isEqualTo(1);
    }

    private RecifradorPorLotes recifrador(RecifradorPorLotes.Almacen almacen, int lote) {
        return new RecifradorPorLotes(almacen, lote,
                valor -> convertidor.convertToDatabaseColumn(convertidor.convertToEntityAttribute(valor)));
    }

    private byte[] bytes() {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        return b;
    }

    /** Almacén en memoria con las mismas reglas que la consulta JDBC. */
    private static final class AlmacenEnMemoria implements RecifradorPorLotes.Almacen {
        private final Map<Object, String> filas = new LinkedHashMap<>();
        private final List<Integer> limitesPedidos = new ArrayList<>();
        private int fallarEnActualizacion;
        private boolean rechazarActualizacion;
        private int actualizaciones;

        void agregar(Object id, String valor) {
            filas.put(id, valor);
        }

        List<String> valores() {
            return new ArrayList<>(filas.values());
        }

        @Override
        public List<RecifradorPorLotes.Fila> pendientes(String tabla, String columna, int limite) {
            limitesPedidos.add(limite);
            String prefijo = "v" + ClavesCripto.versionActiva() + ":";
            List<RecifradorPorLotes.Fila> salida = new ArrayList<>();
            for (Map.Entry<Object, String> e : filas.entrySet()) {
                if (!e.getValue().startsWith(prefijo) && salida.size() < limite) {
                    salida.add(new RecifradorPorLotes.Fila(e.getKey(), e.getValue()));
                }
            }
            return salida;
        }

        @Override
        public boolean actualizar(String tabla, String columna, Object id, String valorAnterior, String valorNuevo) {
            if (fallarEnActualizacion > 0 && ++actualizaciones == fallarEnActualizacion) {
                throw new IllegalStateException("corte simulado");
            }
            if (rechazarActualizacion || !valorAnterior.equals(filas.get(id))) {
                return false;
            }
            filas.put(id, valorNuevo);
            return true;
        }
    }
}
