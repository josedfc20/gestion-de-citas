package com.gestiondecitas.horario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-018: horario general y ajuste por empleado (RN-002). */
@IntegracionTest
class HorariosTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private ApiDePrueba api;
    private String admin;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
        admin = ApiDePrueba.bearerAdmin(jwtService, jdbc);
    }

    @Test
    void guardaYLeeElHorarioGeneral() throws Exception {
        guardarGeneral(List.of(
                dia(1, tramo("16:00", "20:00"), tramo("09:00", "14:00")),
                dia(6, tramo("09:00", "14:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias.length()").value(7))
                .andExpect(jsonPath("$.citasAfectadas").isArray());

        mockMvc.perform(get("/api/v1/admin/horario-general").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias.length()").value(7))
                .andExpect(jsonPath("$.dias[0].diaSemana").value(1))
                .andExpect(jsonPath("$.dias[0].tramos[0].inicio").value("09:00"))
                .andExpect(jsonPath("$.dias[0].tramos[0].fin").value("14:00"))
                .andExpect(jsonPath("$.dias[0].tramos[1].inicio").value("16:00"))
                .andExpect(jsonPath("$.dias[1].tramos").isEmpty())
                .andExpect(jsonPath("$.dias[5].tramos.length()").value(1))
                .andExpect(jsonPath("$.dias[6].tramos").isEmpty())
                .andExpect(jsonPath("$.citasAfectadas").doesNotExist());
    }

    @Test
    void rechazaTramosSolapadosOQueSeTocan() throws Exception {
        guardarGeneral(List.of(dia(2, tramo("09:00", "14:00"), tramo("13:00", "16:00"))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Los tramos del martes se solapan (09:00-14:00 y 13:00-16:00)."));
        guardarGeneral(List.of(dia(2, tramo("09:00", "14:00"), tramo("14:00", "16:00"))))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void rechazaDatosConFormatoIncorrecto() throws Exception {
        guardarGeneral(List.of(dia(1, tramo("14:00", "09:00")))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("La hora de inicio debe ser anterior a la de fin."));
        guardarGeneral(List.of(dia(1, tramo("09:07", "14:00")))).andExpect(status().isBadRequest());
        guardarGeneral(List.of(dia(8, tramo("09:00", "14:00")))).andExpect(status().isBadRequest());
        guardarGeneral(List.of(dia(1, tramo("09:00", "10:00")), dia(1, tramo("11:00", "12:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("Cada día de la semana solo puede aparecer una vez."));
        guardarGeneral(List.of(dia(1, tramo("08:00", "09:00"), tramo("10:00", "11:00"), tramo("12:00", "13:00"),
                tramo("14:00", "15:00"), tramo("16:00", "17:00")))).andExpect(status().isBadRequest());
        guardarGeneral(List.of(dia(1, tramo("9h", "14:00")))).andExpect(status().isBadRequest());
    }

    @Test
    void unaCitaQueQuedaFueraDelNuevoHorarioSeListaYSeMantiene() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long empleado = api.crearEmpleadoConServicios(admin, List.of(corte));
        LocalDate fecha = LocalDate.now(MADRID).plusDays(5);
        long tarde = ApiDePrueba.crearCita(jdbc, clienteNuevo(), empleado, corte, instante(fecha, "17:00"));
        long manana = ApiDePrueba.crearCita(jdbc, clienteNuevo(), empleado, corte, instante(fecha, "10:00"));

        guardarGeneral(todaLaSemana(tramo("09:00", "14:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citasAfectadas[*].id", hasItem((int) tarde)))
                .andExpect(jsonPath("$.citasAfectadas[*].id", not(hasItem((int) manana))))
                .andExpect(jsonPath("$.citasAfectadas[?(@.id == " + tarde + ")].profesional").value("Luis García"))
                .andExpect(jsonPath("$.citasAfectadas[?(@.id == " + tarde + ")].profesionalId").value((int) empleado));
        assertThat(jdbc.queryForObject("SELECT estado FROM cita WHERE id = ?", String.class, tarde))
                .isEqualTo("CONFIRMADA");
    }

    @Test
    void elAjusteDeUnEmpleadoSeMezclaConElHorarioGeneral() throws Exception {
        guardarGeneral(todaLaSemana(tramo("09:00", "14:00"))).andExpect(status().isOk());
        long empleado = api.crearEmpleadoConServicios(admin, List.of());

        enviar(put("/api/v1/admin/empleados/" + empleado + "/horario"), Map.of("dias", List.of(
                dia(1), dia(2, tramo("16:00", "20:00"), tramo("10:00", "13:00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citasAfectadas").isArray());

        mockMvc.perform(get("/api/v1/admin/empleados/" + empleado + "/horario")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empleadoId").value((int) empleado))
                .andExpect(jsonPath("$.ajustes.length()").value(2))
                .andExpect(jsonPath("$.ajustes[0].diaSemana").value(1))
                .andExpect(jsonPath("$.ajustes[0].tramos").isEmpty())
                .andExpect(jsonPath("$.semanaEfectiva.length()").value(7))
                .andExpect(jsonPath("$.semanaEfectiva[0].ajustado").value(true))
                .andExpect(jsonPath("$.semanaEfectiva[0].tramos").isEmpty())
                .andExpect(jsonPath("$.semanaEfectiva[1].tramos[0].inicio").value("10:00"))
                .andExpect(jsonPath("$.semanaEfectiva[1].tramos[1].inicio").value("16:00"))
                .andExpect(jsonPath("$.semanaEfectiva[2].ajustado").value(false))
                .andExpect(jsonPath("$.semanaEfectiva[2].tramos[0].inicio").value("09:00"))
                .andExpect(jsonPath("$.citasAfectadas").doesNotExist());

        // Volver al horario general.
        enviar(put("/api/v1/admin/empleados/" + empleado + "/horario"), Map.of("dias", List.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ajustes").isEmpty())
                .andExpect(jsonPath("$.semanaEfectiva[0].ajustado").value(false))
                .andExpect(jsonPath("$.semanaEfectiva[0].tramos[0].inicio").value("09:00"));
    }

    @Test
    void elHorarioDeEmpleadoSoloExisteParaEmpleados() throws Exception {
        long cliente = clienteNuevo();
        mockMvc.perform(get("/api/v1/admin/empleados/" + cliente + "/horario").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        enviar(put("/api/v1/admin/empleados/999999/horario"), Map.of("dias", List.of()))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElAdminConfiguraLosHorarios() throws Exception {
        var cliente = api.clienteConSesion();
        mockMvc.perform(get("/api/v1/admin/horario-general").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dias-cierre").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/horario-general")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/dias-cierre")).andExpect(status().isUnauthorized());
    }

    static List<Map<String, Object>> todaLaSemana(Map<String, String> tramo) {
        return IntStream.rangeClosed(1, 7).mapToObj(d -> dia(d, tramo)).toList();
    }

    @SafeVarargs
    static Map<String, Object> dia(int diaSemana, Map<String, String>... tramos) {
        return Map.of("diaSemana", diaSemana, "tramos", List.of(tramos));
    }

    static Map<String, String> tramo(String inicio, String fin) {
        return Map.of("inicio", inicio, "fin", fin);
    }

    static Instant instante(LocalDate fecha, String hora) {
        return ZonedDateTime.of(fecha, LocalTime.parse(hora), MADRID).toInstant();
    }

    private ResultActions guardarGeneral(List<Map<String, Object>> dias) throws Exception {
        return enviar(put("/api/v1/admin/horario-general"), Map.of("dias", dias));
    }

    private long clienteNuevo() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion,
            Object cuerpo) throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(cuerpo)));
    }
}
