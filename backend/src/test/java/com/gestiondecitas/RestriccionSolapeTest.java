package com.gestiondecitas;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RNF-003 en la base de datos: la restricción {@code no_solape_profesional}
 * impide dos citas activas solapadas del mismo profesional.
 */
@IntegracionTest
class RestriccionSolapeTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long clienteId;
    private long empleadoId;
    private long otroEmpleadoId;
    private long servicioId;

    @BeforeEach
    void prepararDatos() {
        jdbc.execute("TRUNCATE usuario, servicio RESTART IDENTITY CASCADE");
        clienteId = crearUsuario("CLIENTE", "cliente@ejemplo.com");
        jdbc.update("INSERT INTO cliente (usuario_id, fecha_nacimiento, privacidad_aceptada_en) "
                + "VALUES (?, DATE '1990-01-01', now())", clienteId);
        empleadoId = crearEmpleado("empleado@ejemplo.com");
        otroEmpleadoId = crearEmpleado("otro@ejemplo.com");
        servicioId = jdbc.queryForObject("INSERT INTO servicio (nombre, duracion_minutos, precio) "
                + "VALUES ('Servicio', 30, 15.00) RETURNING id", Long.class);
    }

    @Test
    void rechazaDosCitasSolapadasDelMismoProfesional() {
        reservar(empleadoId, "10:00", "10:30", "CONFIRMADA");

        assertThatThrownBy(() -> reservar(empleadoId, "10:15", "10:45", "CONFIRMADA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("no_solape_profesional");
    }

    @Test
    void permiteCitasSeguidas() {
        reservar(empleadoId, "10:00", "10:30", "CONFIRMADA");

        assertThatCode(() -> reservar(empleadoId, "10:30", "11:00", "CONFIRMADA")).doesNotThrowAnyException();
    }

    @Test
    void permiteLaMismaHoraConOtroProfesional() {
        reservar(empleadoId, "10:00", "10:30", "CONFIRMADA");

        assertThatCode(() -> reservar(otroEmpleadoId, "10:00", "10:30", "CONFIRMADA")).doesNotThrowAnyException();
    }

    @Test
    void unaCitaCanceladaLiberaElHueco() {
        reservar(empleadoId, "10:00", "10:30", "CANCELADA");

        assertThatCode(() -> reservar(empleadoId, "10:00", "10:30", "CONFIRMADA")).doesNotThrowAnyException();
    }

    private long crearUsuario(String rol, String email) {
        return jdbc.queryForObject("INSERT INTO usuario (rol, nombre, apellidos, email, password_hash) "
                + "VALUES (?, 'Nombre', 'Apellidos', ?, 'hash') RETURNING id", Long.class, rol, email);
    }

    private long crearEmpleado(String email) {
        long id = crearUsuario("EMPLEADO", email);
        jdbc.update("INSERT INTO empleado (usuario_id) VALUES (?)", id);
        return id;
    }

    private void reservar(long empleado, String inicio, String fin, String estado) {
        jdbc.update("INSERT INTO cita (cliente_id, empleado_id, servicio_id, inicio, fin, precio, estado) "
                + "VALUES (?, ?, ?, ?::timestamptz, ?::timestamptz, 15.00, ?)",
                clienteId, empleado, servicioId,
                "2030-01-07 " + inicio + ":00+01", "2030-01-07 " + fin + ":00+01", estado);
    }
}
