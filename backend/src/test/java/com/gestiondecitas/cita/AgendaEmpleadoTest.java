package com.gestiondecitas.cita;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.ReservasDePrueba;
import com.gestiondecitas.ReservasDePrueba.Cliente;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-013 y CU-014: agenda y citas asignadas del empleado. */
@IntegracionTest
class AgendaEmpleadoTest {

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

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM dia_cierre");
    }

    @Test
    void laAgendaMuestraElHorarioYLasCitasNoCanceladasPorDia() throws Exception {
        long lucia = r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", lucia).andExpect(status().isCreated());
        r.reservarCorte(cliente, "17:00");
        long cancelada = r.reservarCorte(r.cliente(), "12:00");
        r.enviar(patch("/api/v1/citas/" + cancelada + "/estado"), empleado,
                Map.of("estado", "CANCELADA", "motivo", "Cambio de planes")).andExpect(status().isOk());
        // El día siguiente libra (ajuste sin tramos) y el otro cierra el negocio.
        r.enviar(put("/api/v1/admin/empleados/" + r.empleado + "/horario"), r.admin, Map.of("dias", List.of(
                Map.of("diaSemana", r.fecha.plusDays(1).getDayOfWeek().getValue(), "tramos", List.of()))))
                .andExpect(status().isOk());
        r.enviar(post("/api/v1/admin/dias-cierre"), r.admin, Map.of("fecha", r.fecha.plusDays(2).toString()))
                .andExpect(status().isCreated());

        r.leer("/api/v1/empleado/agenda?desde=" + r.fecha + "&hasta=" + r.fecha.plusDays(2), empleado)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fecha").value(r.fecha.toString()))
                .andExpect(jsonPath("$[0].cierre").value(false))
                .andExpect(jsonPath("$[0].tramos.length()").value(2))
                .andExpect(jsonPath("$[0].tramos[0].inicio").value("09:00"))
                .andExpect(jsonPath("$[0].citas.length()").value(2))
                .andExpect(jsonPath("$[0].citas[0].beneficiario.nombre").value("Lucía"))
                .andExpect(jsonPath("$[0].citas[0].cliente.telefono").value("600123123"))
                .andExpect(jsonPath("$[0].citas[1].inicio").value(ReservasDePrueba.inicio(r.fecha, "17:00")))
                .andExpect(jsonPath("$[1].tramos").isEmpty())
                .andExpect(jsonPath("$[1].cierre").value(false))
                .andExpect(jsonPath("$[2].cierre").value(true))
                .andExpect(jsonPath("$[2].tramos").isEmpty());
    }

    @Test
    void sinFechasEsHoyYElRangoTieneLimite() throws Exception {
        r.leer("/api/v1/empleado/agenda", empleado)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fecha").value(r.hoy.toString()));
        r.leer("/api/v1/empleado/agenda?desde=" + r.hoy + "&hasta=" + r.hoy.plusDays(30), empleado)
                .andExpect(jsonPath("$.length()").value(31));
        r.leer("/api/v1/empleado/agenda?desde=" + r.hoy + "&hasta=" + r.hoy.plusDays(31), empleado)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La agenda se consulta como mucho 31 días seguidos."));
        r.leer("/api/v1/empleado/agenda?desde=" + r.hoy + "&hasta=" + r.hoy.minusDays(1), empleado)
                .andExpect(status().isBadRequest());
    }

    @Test
    void laListaDeCitasFiltraPorEstadoYFechas() throws Exception {
        long primera = r.reservarCorte(cliente, "10:00");
        long segunda = r.reservarCorte(r.cliente(), "11:00");
        r.enviar(patch("/api/v1/citas/" + segunda + "/estado"), empleado,
                Map.of("estado", "CANCELADA", "motivo", "Avería")).andExpect(status().isOk());
        long otroDia = r.api().leer(r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha.plusDays(1), "10:00",
                null).andExpect(status().isCreated()).andReturn()).get("id").asLong();
        r.reservar(r.cliente().bearer(), r.corte, r.otroEmpleado(), r.fecha, "10:00", null)
                .andExpect(status().isCreated()); // de otro profesional

        r.leer("/api/v1/empleado/citas", empleado)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[0].id").value(primera))
                .andExpect(jsonPath("$.contenido[0].cliente.nombre").value("Ana"))
                .andExpect(jsonPath("$.contenido[2].id").value(otroDia));
        r.leer("/api/v1/empleado/citas?estado=CANCELADA", empleado)
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(segunda));
        r.leer("/api/v1/empleado/citas?desde=" + r.fecha + "&hasta=" + r.fecha, empleado)
                .andExpect(jsonPath("$.totalElementos").value(2));
        r.leer("/api/v1/empleado/citas?estado=PERDIDA", empleado).andExpect(status().isBadRequest());
    }

    @Test
    void soloLosEmpleadosTienenAgenda() throws Exception {
        r.leer("/api/v1/empleado/agenda", cliente.bearer()).andExpect(status().isForbidden());
        r.leer("/api/v1/empleado/citas", r.admin).andExpect(status().isForbidden());
    }
}
