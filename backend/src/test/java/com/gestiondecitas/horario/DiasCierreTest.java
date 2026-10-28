package com.gestiondecitas.horario;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-018: días de cierre del negocio y de un empleado. */
@IntegracionTest
class DiasCierreTest {

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
    private LocalDate hoy;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
        admin = ApiDePrueba.bearerAdmin(jwtService, jdbc);
        hoy = LocalDate.now(MADRID);
    }

    /** Los cierres del negocio afectan a todas las pruebas de disponibilidad: no se dejan puestos. */
    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM dia_cierre");
    }

    @Test
    void creaUnCierreDelNegocioYNoLoRepite() throws Exception {
        LocalDate fecha = hoy.plusDays(20);

        crear(fecha, null, "  Fiesta local ")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        org.hamcrest.Matchers.matchesPattern(".*/api/v1/admin/dias-cierre/\\d+")))
                .andExpect(jsonPath("$.fecha").value(fecha.toString()))
                .andExpect(jsonPath("$.empleadoId").doesNotExist())
                .andExpect(jsonPath("$.motivo").value("Fiesta local"))
                .andExpect(jsonPath("$.citasAfectadas").isArray());

        crear(fecha, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese día de cierre ya existe."));
    }

    @Test
    void elMismoDiaPuedeCerrarElNegocioYUnEmpleadoPorSeparado() throws Exception {
        long empleado = api.crearEmpleadoConServicios(admin, List.of());
        LocalDate fecha = hoy.plusDays(21);
        crear(fecha, null, null).andExpect(status().isCreated());

        crear(fecha, empleado, "Vacaciones")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empleadoId").value((int) empleado))
                .andExpect(jsonPath("$.empleado").value("Luis García"));
        crear(fecha, empleado, null).andExpect(status().isConflict());
    }

    @Test
    void rechazaFechasPasadasEmpleadosInexistentesYMotivosLargos() throws Exception {
        crear(hoy.minusDays(1), null, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La fecha ya ha pasado."));
        crear(hoy, null, null).andExpect(status().isCreated());
        crear(hoy.plusDays(1), 999999L, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("El empleado no existe."));
        crear(hoy.plusDays(1), null, "x".repeat(201)).andExpect(status().isBadRequest());
        enviar(Map.of("motivo", "Sin fecha")).andExpect(status().isBadRequest());
    }

    @Test
    void listaYFiltraPorFechas() throws Exception {
        long empleado = api.crearEmpleadoConServicios(admin, List.of());
        crear(hoy.plusDays(3), null, "Festivo").andExpect(status().isCreated());
        crear(hoy.plusDays(10), empleado, "Vacaciones").andExpect(status().isCreated());
        jdbc.update("INSERT INTO dia_cierre (negocio_id, fecha) VALUES ((SELECT MIN(id) FROM negocio), ?)",
                hoy.minusDays(5));

        mockMvc.perform(get("/api/v1/admin/dias-cierre").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value(hoy.plusDays(3).toString()))
                .andExpect(jsonPath("$[1].empleado").value("Luis García"));
        mockMvc.perform(get("/api/v1/admin/dias-cierre").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("desde", hoy.minusDays(30).toString()).param("hasta", hoy.plusDays(5).toString()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value(hoy.minusDays(5).toString()));
    }

    @Test
    void borraUnCierre() throws Exception {
        long id = api.leer(crear(hoy.plusDays(4), null, null).andReturn()).get("id").asLong();

        mockMvc.perform(get("/api/v1/admin/dias-cierre/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citasAfectadas").doesNotExist());
        mockMvc.perform(delete("/api/v1/admin/dias-cierre/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/admin/dias-cierre/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/admin/dias-cierre/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaLasCitasConfirmadasDeEseDia() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long empleado = api.crearEmpleadoConServicios(admin, List.of(corte));
        long cliente = clienteNuevo();
        LocalDate fecha = hoy.plusDays(6);
        long eseDia = ApiDePrueba.crearCita(jdbc, cliente, empleado, corte, HorariosTest.instante(fecha, "10:00"));
        long otroDia = ApiDePrueba.crearCita(jdbc, cliente, empleado, corte,
                HorariosTest.instante(fecha.plusDays(1), "10:00"));

        crear(fecha, empleado, "Baja")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.citasAfectadas.length()").value(1))
                .andExpect(jsonPath("$.citasAfectadas[0].id").value((int) eseDia))
                .andExpect(jsonPath("$.citasAfectadas[0].inicio").value(org.hamcrest.Matchers.startsWith(
                        fecha + "T10:00:00+0")))
                .andExpect(jsonPath("$.citasAfectadas[*].id", not(hasItem((int) otroDia))));
    }

    private ResultActions crear(LocalDate fecha, Long empleadoId, String motivo) throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("fecha", fecha.toString());
        cuerpo.put("empleadoId", empleadoId);
        cuerpo.put("motivo", motivo);
        return enviar(cuerpo);
    }

    private ResultActions enviar(Object cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/dias-cierre")
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(cuerpo)));
    }

    private long clienteNuevo() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }
}
