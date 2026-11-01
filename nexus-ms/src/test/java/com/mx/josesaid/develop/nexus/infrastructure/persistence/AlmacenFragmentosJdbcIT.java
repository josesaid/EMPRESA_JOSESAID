package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.mx.josesaid.develop.nexus.application.FragmentoIndexado;
import com.mx.josesaid.develop.nexus.application.ResultadoPolitica;

/** Prueba el filtro por rol y vigencia contra PostgreSQL real con pgvector. */
@Testcontainers
class AlmacenFragmentosJdbcIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    private static final LocalDate HOY = LocalDate.of(2026, 11, 2);

    private AlmacenFragmentosJdbc almacen;

    @BeforeEach
    void prepararEsquema() {
        DriverManagerDataSource ds = new DriverManagerDataSource(postgres.getJdbcUrl(),
                postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.update("DELETE FROM fragmentos_politica");
        almacen = new AlmacenFragmentosJdbc(jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(ds)));
    }

    @Test
    void soloDevuelvePoliticasDeLosRolesDelUsuario() {
        almacen.reemplazar("compras", List.of(fragmento("compras", List.of("COMPRAS", "DIRECCION"),
                LocalDate.of(2026, 11, 1), 0)));
        almacen.reemplazar("finanzas", List.of(fragmento("finanzas", List.of("FINANZAS"),
                LocalDate.of(2026, 11, 1), 0)));

        List<ResultadoPolitica> paraCompras = almacen.buscar(Set.of("COMPRAS"), vector(0), HOY, 5);
        List<ResultadoPolitica> paraFinanzas = almacen.buscar(Set.of("FINANZAS"), vector(0), HOY, 5);

        assertThat(paraCompras).extracting(ResultadoPolitica::politicaId).containsExactly("compras");
        assertThat(paraFinanzas).extracting(ResultadoPolitica::politicaId).containsExactly("finanzas");
    }

    @Test
    void rolSinPoliticasNoRecibeNada() {
        almacen.reemplazar("compras", List.of(fragmento("compras", List.of("COMPRAS"),
                LocalDate.of(2026, 11, 1), 0)));

        assertThat(almacen.buscar(Set.of("VENTAS"), vector(0), HOY, 5)).isEmpty();
    }

    @Test
    void noDevuelvePoliticasQueAunNoEstanVigentes() {
        almacen.reemplazar("futura", List.of(fragmento("futura", List.of("COMPRAS"),
                LocalDate.of(2027, 1, 1), 0)));

        assertThat(almacen.buscar(Set.of("COMPRAS"), vector(0), HOY, 5)).isEmpty();
    }

    @Test
    void ordenaPorSimilitudYRespetaElLimite() {
        almacen.reemplazar("compras", List.of(
                fragmento("compras", List.of("COMPRAS"), LocalDate.of(2026, 11, 1), 0),
                fragmento("compras", List.of("COMPRAS"), LocalDate.of(2026, 11, 1), 1)));

        List<ResultadoPolitica> resultados = almacen.buscar(Set.of("COMPRAS"), vector(1), HOY, 1);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).indice()).isEqualTo(1);
        assertThat(resultados.get(0).similitud()).isGreaterThan(0.99);
    }

    @Test
    void reemplazarBorraLosFragmentosAnteriores() {
        almacen.reemplazar("compras", List.of(
                fragmento("compras", List.of("COMPRAS"), LocalDate.of(2026, 11, 1), 0),
                fragmento("compras", List.of("COMPRAS"), LocalDate.of(2026, 11, 1), 1)));
        almacen.reemplazar("compras", List.of(fragmento("compras", List.of("COMPRAS"),
                LocalDate.of(2026, 11, 1), 0)));

        assertThat(almacen.buscar(Set.of("COMPRAS"), vector(0), HOY, 10)).hasSize(1);
    }

    private static FragmentoIndexado fragmento(String politica, List<String> roles, LocalDate vigencia, int indice) {
        return new FragmentoIndexado(politica, indice, "Título " + politica, vigencia, roles,
                "regla " + indice, vector(indice));
    }

    /** Vector con un 1 en la posición indicada: dos vectores distintos tienen similitud baja. */
    private static float[] vector(int posicion) {
        float[] v = new float[1024];
        v[posicion] = 1f;
        return v;
    }
}
