package com.mx.josesaid.develop.clientes.infrastructure.crypto;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementación JDBC de {@link RecifradorPorLotes.Almacen}. Tabla y columna vienen de constantes del código
 * (nunca de entrada del usuario), así que se componen en el SQL; los valores van como parámetros.
 */
@Component
public class RecifradorJdbc implements RecifradorPorLotes.Almacen {

    private final JdbcTemplate jdbc;

    /** Acceso JDBC para el re-cifrado por lotes. */
    public RecifradorJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Devuelve hasta el límite de filas cuyo valor aún no está en la versión activa. */
    @Override
    public List<RecifradorPorLotes.Fila> pendientes(String tabla, String columna, int limite) {
        String sql = "SELECT id, " + columna + " AS valor FROM " + tabla
                + " WHERE " + columna + " IS NOT NULL AND " + columna + " NOT LIKE ? ORDER BY id LIMIT ?";
        return jdbc.query(sql,
                (resultado, fila) -> new RecifradorPorLotes.Fila(resultado.getObject("id"), resultado.getString("valor")),
                "v" + ClavesCripto.versionActiva() + ":%",
                limite);
    }

    /** Cambia el valor de una fila solo si aún vale lo que se leyó. */
    @Override
    public boolean actualizar(String tabla, String columna, Object id, String valorAnterior, String valorNuevo) {
        String sql = "UPDATE " + tabla + " SET " + columna + " = ? WHERE id = ? AND " + columna + " = ?";
        return jdbc.update(sql, valorNuevo, id, valorAnterior) == 1;
    }
}
