package com.gestiondecitas.cita;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Implementación provisional sobre la tabla {@code cita} (V1) hasta que B6 cree la entidad. */
@Component
class OcupacionesProfesionalJdbc implements OcupacionesProfesional {

    private static final String SELECT = """
            SELECT c.id, c.empleado_id, u.nombre || ' ' || u.apellidos AS profesional, c.inicio, c.fin
            FROM cita c JOIN usuario u ON u.id = c.empleado_id
            """;

    private final JdbcClient jdbc;

    OcupacionesProfesionalJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CitaOcupada> deEmpleado(Long empleadoId, Instant desde, Instant hasta) {
        return jdbc.sql(SELECT + """
                        WHERE c.empleado_id = ? AND c.estado IN ('CONFIRMADA', 'COMPLETADA')
                          AND c.inicio < ? AND c.fin > ?
                        ORDER BY c.inicio""")
                .params(empleadoId, Timestamp.from(hasta), Timestamp.from(desde))
                .query(OcupacionesProfesionalJdbc::fila).list();
    }

    @Override
    public List<CitaOcupada> confirmadasFuturas(Long empleadoId, Instant desde) {
        if (empleadoId == null) {
            return jdbc.sql(SELECT + "WHERE c.estado = 'CONFIRMADA' AND c.inicio > ? ORDER BY c.inicio")
                    .params(Timestamp.from(desde))
                    .query(OcupacionesProfesionalJdbc::fila).list();
        }
        return jdbc.sql(SELECT + """
                        WHERE c.empleado_id = ? AND c.estado = 'CONFIRMADA' AND c.inicio > ?
                        ORDER BY c.inicio""")
                .params(empleadoId, Timestamp.from(desde))
                .query(OcupacionesProfesionalJdbc::fila).list();
    }

    private static CitaOcupada fila(ResultSet rs, int numero) throws SQLException {
        return new CitaOcupada(rs.getLong("id"), rs.getLong("empleado_id"), rs.getString("profesional").trim(),
                rs.getTimestamp("inicio").toInstant(), rs.getTimestamp("fin").toInstant());
    }
}
