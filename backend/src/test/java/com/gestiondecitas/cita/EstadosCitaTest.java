package com.gestiondecitas.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.ReservasDePrueba;
import com.gestiondecitas.ReservasDePrueba.Cliente;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-015 y CU-019: cambiar el estado de una cita (RN-004, RN-008). */
@IntegracionTest
class EstadosCitaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private ReservasDePrueba r;
    private Cliente cliente;
    private String empleado;

    @BeforeEach
    void preparar() throws Exception {
        r = new ReservasDePrueba(mockMvc, jsonMapper, jdbc, jwtService);
        cliente = r.cliente();
        empleado = r.bearerEmpleado(r.empleado);
    }

    @Test
    void elEmpleadoCompletaUnaCitaYaEmpezadaYQuedaRegistrado() throws Exception {
        long cita = ApiDePrueba.crearCita(jdbc, cliente.id(), r.empleado, r.corte,
                Instant.now().minus(Duration.ofMinutes(10)));

        cambiar(empleado, cita, "COMPLETADA", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.cliente.nombre").value("Ana"));

        assertThat(jdbc.queryForMap("SELECT estado_anterior, estado_nuevo, realizado_por FROM cambio_estado_cita "
                + "WHERE cita_id = ?", cita))
                .containsEntry("estado_anterior", "CONFIRMADA")
                .containsEntry("estado_nuevo", "COMPLETADA")
                .containsEntry("realizado_por", r.empleado);
        cambiar(empleado, cita, "NO_PRESENTADO", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Esta cita ya no se puede cambiar: no está confirmada."));
    }

    @Test
    void noSeCompletaNiSeMarcaNoPresentadoAntesDeLaHora() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");

        cambiar(empleado, cita, "COMPLETADA", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("No se puede marcar como completada antes de la hora de inicio."));
        cambiar(empleado, cita, "NO_PRESENTADO", null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void cancelarPideMotivoYLiberaElHueco() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");

        cambiar(empleado, cita, "CANCELADA", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Indica el motivo de la cancelación."));
        cambiar(empleado, cita, "CANCELADA", "Baja del profesional")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelacion").value("Baja del profesional"));
        r.disponibilidad(r.corte, r.empleado, r.fecha).andExpect(jsonPath("$.horas", hasItem("10:00")));
        // El admin ve en el historial quién hizo cada cambio (B9b).
        r.leer("/api/v1/citas/" + cita, r.admin)
                .andExpect(jsonPath("$.cambios[0].realizadoPorNombre").value("Cliente"))
                .andExpect(jsonPath("$.cambios[1].realizadoPor").value(r.empleado))
                .andExpect(jsonPath("$.cambios[1].realizadoPorNombre").value("Luis G."));
    }

    @Test
    void elAdminCancelaConMenosDe24HorasYVeElHistorial() throws Exception {
        long cita = ApiDePrueba.crearCita(jdbc, cliente.id(), r.empleado, r.corte,
                Instant.now().plus(Duration.ofHours(2)));
        jdbc.update("INSERT INTO cambio_estado_cita (cita_id, estado_nuevo, realizado_por) VALUES (?, 'CONFIRMADA', ?)",
                cita, cliente.id());

        cambiar(r.admin, cita, "CANCELADA", "El cliente llamó por teléfono")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cliente.email").value(cliente.email()))
                .andExpect(jsonPath("$.cambios.length()").value(2))
                .andExpect(jsonPath("$.cambios[1].estadoAnterior").value("CONFIRMADA"))
                .andExpect(jsonPath("$.cambios[1].estadoNuevo").value("CANCELADA"));
    }

    @Test
    void elEmpleadoSoloCambiaSusCitasYElClienteNinguna() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");

        cambiar(r.bearerEmpleado(r.otroEmpleado()), cita, "CANCELADA", "No es mía")
                .andExpect(status().isNotFound());
        cambiar(cliente.bearer(), cita, "CANCELADA", "Quiero cancelar").andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/citas/" + cita + "/estado")).andExpect(status().isUnauthorized());
        cambiar(empleado, 999999L, "CANCELADA", "No existe").andExpect(status().isNotFound());
    }

    @Test
    void unEstadoDesconocidoOVacioEs400() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");

        cambiar(empleado, cita, "PERDIDA", null).andExpect(status().isBadRequest());
        cambiar(empleado, cita, null, "Motivo").andExpect(status().isBadRequest());
        cambiar(empleado, cita, "CONFIRMADA", null).andExpect(status().isUnprocessableContent());
    }

    private ResultActions cambiar(String bearer, long cita, String estado, String motivo) throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("estado", estado);
        cuerpo.put("motivo", motivo);
        return r.enviar(patch("/api/v1/citas/" + cita + "/estado"), bearer, cuerpo);
    }
}
