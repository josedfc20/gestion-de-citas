package com.gestiondecitas.cita;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Implementación provisional sobre la tabla {@code cita} (V1) hasta que B6 cree la entidad. */
@Component
class CitasFuturasJdbc implements CitasFuturas {

    private final JdbcClient jdbc;

    CitasFuturasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long deEmpleado(Long empleadoId, Instant desde) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM cita
                        WHERE empleado_id = ? AND estado = 'CONFIRMADA' AND inicio > ?""")
                .params(empleadoId, Timestamp.from(desde))
                .query(Long.class).single();
    }

    @Override
    public long deEmpleadoYServicio(Long empleadoId, Long servicioId, Instant desde) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM cita
                        WHERE empleado_id = ? AND servicio_id = ? AND estado = 'CONFIRMADA' AND inicio > ?""")
                .params(empleadoId, servicioId, Timestamp.from(desde))
                .query(Long.class).single();
    }
}
