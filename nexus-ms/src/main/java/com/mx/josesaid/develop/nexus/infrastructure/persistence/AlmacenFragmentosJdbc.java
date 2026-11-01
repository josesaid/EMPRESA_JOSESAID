package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.mx.josesaid.develop.nexus.application.AlmacenFragmentos;
import com.mx.josesaid.develop.nexus.application.FragmentoIndexado;
import com.mx.josesaid.develop.nexus.application.ResultadoPolitica;

/** Implementación sobre PostgreSQL con pgvector. El filtro por rol y vigencia va en SQL, antes de ordenar por similitud. */
public final class AlmacenFragmentosJdbc implements AlmacenFragmentos {

    private static final String INSERTAR = "INSERT INTO fragmentos_politica "
            + "(id, politica_id, indice, titulo, vigencia, roles, texto, embedding) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS vector))";

    private static final String BUSCAR = "SELECT politica_id, indice, titulo, texto, "
            + "1 - (embedding <=> CAST(? AS vector)) AS similitud "
            + "FROM fragmentos_politica "
            + "WHERE roles && CAST(? AS text[]) AND vigencia <= ? "
            + "ORDER BY embedding <=> CAST(? AS vector) "
            + "LIMIT ?";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    public AlmacenFragmentosJdbc(JdbcTemplate jdbc, TransactionTemplate transacciones) {
        this.jdbc = jdbc;
        this.transacciones = transacciones;
    }

    @Override
    public void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos) {
        transacciones.executeWithoutResult(estado -> {
            jdbc.update("DELETE FROM fragmentos_politica WHERE politica_id = ?", politicaId);
            if (fragmentos.isEmpty()) {
                return;
            }
            jdbc.batchUpdate(INSERTAR, fragmentos, fragmentos.size(), (ps, f) -> {
                ps.setObject(1, UUID.randomUUID());
                ps.setString(2, f.politicaId());
                ps.setInt(3, f.indice());
                ps.setString(4, f.titulo());
                ps.setDate(5, Date.valueOf(f.vigencia()));
                ps.setArray(6, ps.getConnection().createArrayOf("text", f.roles().toArray(String[]::new)));
                ps.setString(7, f.texto());
                ps.setString(8, aLiteral(f.vector()));
            });
        });
    }

    @Override
    public List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite) {
        String vector = aLiteral(consulta);
        return jdbc.query(BUSCAR, ps -> {
            ps.setString(1, vector);
            ps.setArray(2, ps.getConnection().createArrayOf("text", roles.toArray(String[]::new)));
            ps.setDate(3, Date.valueOf(hoy));
            ps.setString(4, vector);
            ps.setInt(5, limite);
        }, (rs, n) -> new ResultadoPolitica(rs.getString("politica_id"), rs.getInt("indice"),
                rs.getString("titulo"), rs.getString("texto"), rs.getDouble("similitud")));
    }

    /** Formato de pgvector: [0.1,0.2,...] */
    static String aLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
