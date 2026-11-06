package com.mx.josesaid.develop.compras.infrastructure.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecifradorPorLotesTest {

    private static final String IDX = Base64.getEncoder().encodeToString(new byte[32]);
    private final SecureRandom aleatorio = new SecureRandom();

    private String clave() {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private ProtectorDatos protector(String k1, String k2, int activa) {
        return new ProtectorDatos("", "1:" + k1 + ",2:" + k2, activa, IDX);
    }

    @Test
    void recifraLosRfcPendientesYLosDejaDescifrablesConLaVersionNueva() {
        String k1 = clave();
        String k2 = clave();
        ProtectorDatos vieja = protector(k1, k2, 1);
        ProtectorDatos nueva = protector(k1, k2, 2);
        AlmacenEnMemoria almacen = new AlmacenEnMemoria(nueva);
        almacen.agregar(UUID.randomUUID(), vieja.cifrar("AAN010101AB3"));
        almacen.agregar(UUID.randomUUID(), vieja.cifrar("PEGJ800101AAA"));

        int recifradas = new RecifradorPorLotes(almacen, 10, v -> nueva.cifrar(nueva.descifrar(v)))
                .ejecutar("proveedores", "rfc_cifrado");

        assertThat(recifradas).isEqualTo(2);
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(v).startsWith("v2:"));
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(nueva.descifrar(v))
                .isIn("AAN010101AB3", "PEGJ800101AAA"));
    }

    @Test
    void respetaElTamanoDeLote() {
        String k1 = clave();
        String k2 = clave();
        ProtectorDatos vieja = protector(k1, k2, 1);
        ProtectorDatos nueva = protector(k1, k2, 2);
        AlmacenEnMemoria almacen = new AlmacenEnMemoria(nueva);
        for (int i = 0; i < 5; i++) {
            almacen.agregar(UUID.randomUUID(), vieja.cifrar("dato" + i));
        }

        new RecifradorPorLotes(almacen, 2, v -> nueva.cifrar(nueva.descifrar(v))).ejecutar("proveedores", "correo_cifrado");

        assertThat(almacen.limitesPedidos).containsOnly(2);
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(v).startsWith("v2:"));
    }

    @Test
    void reanudarDespuesDeUnCorteNoDuplicaNiPierde() {
        String k1 = clave();
        String k2 = clave();
        ProtectorDatos vieja = protector(k1, k2, 1);
        ProtectorDatos nueva = protector(k1, k2, 2);
        AlmacenEnMemoria almacen = new AlmacenEnMemoria(nueva);
        almacen.agregar(UUID.randomUUID(), vieja.cifrar("uno"));
        almacen.agregar(UUID.randomUUID(), vieja.cifrar("dos"));
        almacen.fallarEnActualizacion = 2;
        RecifradorPorLotes recifrador = new RecifradorPorLotes(almacen, 1, v -> nueva.cifrar(nueva.descifrar(v)));

        assertThatThrownBy(() -> recifrador.ejecutar("proveedores", "rfc_cifrado"))
                .isInstanceOf(IllegalStateException.class);
        almacen.fallarEnActualizacion = 0;
        int segunda = recifrador.ejecutar("proveedores", "rfc_cifrado");

        assertThat(segunda).isEqualTo(1);
        assertThat(almacen.valores()).allSatisfy(v -> assertThat(v).startsWith("v2:"));
    }

    @Test
    void siUnaFilaCambiaDuranteElProcesoSeAvisa() {
        String k1 = clave();
        String k2 = clave();
        ProtectorDatos vieja = protector(k1, k2, 1);
        ProtectorDatos nueva = protector(k1, k2, 2);
        AlmacenEnMemoria almacen = new AlmacenEnMemoria(nueva);
        almacen.agregar(UUID.randomUUID(), vieja.cifrar("x"));
        almacen.rechazarActualizacion = true;

        assertThatThrownBy(() -> new RecifradorPorLotes(almacen, 10, v -> nueva.cifrar(nueva.descifrar(v)))
                .ejecutar("proveedores", "rfc_cifrado"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cambió durante el proceso");
    }

    /** Almacén en memoria con las mismas reglas que la consulta JDBC. */
    private static final class AlmacenEnMemoria implements RecifradorPorLotes.Almacen {
        private final ProtectorDatos protector;
        private final Map<Object, String> filas = new LinkedHashMap<>();
        private final List<Integer> limitesPedidos = new ArrayList<>();
        private int fallarEnActualizacion;
        private int actualizaciones;
        private boolean rechazarActualizacion;

        AlmacenEnMemoria(ProtectorDatos protector) {
            this.protector = protector;
        }

        void agregar(Object id, String valor) {
            filas.put(id, valor);
        }

        List<String> valores() {
            return new ArrayList<>(filas.values());
        }

        @Override
        public List<RecifradorPorLotes.Fila> pendientes(String tabla, String columna, int limite) {
            limitesPedidos.add(limite);
            String prefijo = "v" + protector.versionActiva() + ":";
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
