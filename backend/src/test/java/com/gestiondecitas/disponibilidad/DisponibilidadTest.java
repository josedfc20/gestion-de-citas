package com.gestiondecitas.disponibilidad;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
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

/** CU-007: consultar horas disponibles (RN-002, RN-009, RN-010). */
@IntegracionTest
class DisponibilidadTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final Map<String, String> MANANA = Map.of("inicio", "09:00", "fin", "14:00");
    private static final Map<String, String> TARDE = Map.of("inicio", "16:00", "fin", "20:00");

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
    private long corte;
    private long tinte;
    private long empleado;
    private LocalDate hoy;
    private LocalDate fecha;

    /** Todos los días de 09:00 a 14:00 y de 16:00 a 20:00; así la prueba no depende del día de la semana. */
    @BeforeEach
    void preparar() throws Exception {
        api = new ApiDePrueba(mockMvc, jsonMapper);
        admin = ApiDePrueba.bearerAdmin(jwtService, jdbc);
        enviarAdmin(put("/api/v1/admin/horario-general"), Map.of("dias", IntStream.rangeClosed(1, 7)
                .mapToObj(d -> Map.of("diaSemana", d, "tramos", List.of(MANANA, TARDE))).toList()))
                .andExpect(status().isOk());
        corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        tinte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Tinte"), 60, "40.00");
        empleado = api.crearEmpleadoConServicios(admin, List.of(corte, tinte));
        hoy = LocalDate.now(MADRID);
        fecha = hoy.plusDays(3);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM dia_cierre");
    }

    @Test
    void sinSesionDevuelveLasHorasDelHorarioGeneral() throws Exception {
        consultar(corte, empleado, fecha)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(fecha.toString()))
                .andExpect(jsonPath("$.zonaHoraria").value("Europe/Madrid"))
                .andExpect(jsonPath("$.horas.length()").value(18))
                .andExpect(jsonPath("$.horas[0]").value("09:00"))
                .andExpect(jsonPath("$.horas[9]").value("13:30"))
                .andExpect(jsonPath("$.horas[10]").value("16:00"))
                .andExpect(jsonPath("$.horas[17]").value("19:30"))
                .andExpect(jsonPath("$.siguienteFechaConHuecos").doesNotExist());
    }

    @Test
    void lasCitasConfirmadasOcupanSuHueco() throws Exception {
        ApiDePrueba.crearCita(jdbc, clienteNuevo(), empleado, corte, instante(fecha, "10:00"));

        consultar(corte, empleado, fecha)
                .andExpect(jsonPath("$.horas", not(hasItem("10:00"))))
                .andExpect(jsonPath("$.horas", hasItems("09:30", "10:30")));
        consultar(tinte, empleado, fecha)
                .andExpect(jsonPath("$.horas", not(hasItem("09:30"))))
                .andExpect(jsonPath("$.horas", not(hasItem("10:00"))))
                .andExpect(jsonPath("$.horas", hasItems("09:00", "10:30", "13:00")))
                .andExpect(jsonPath("$.horas", not(hasItem("13:30")))); // no cabe antes de las 14:00
    }

    @Test
    void unaCitaCanceladaNoOcupa() throws Exception {
        long cita = ApiDePrueba.crearCita(jdbc, clienteNuevo(), empleado, corte, instante(fecha, "10:00"));
        jdbc.update("UPDATE cita SET estado = 'CANCELADA' WHERE id = ?", cita);

        consultar(corte, empleado, fecha).andExpect(jsonPath("$.horas", hasItem("10:00")));
    }

    @Test
    void usaElAjusteDelEmpleadoEseDia() throws Exception {
        enviarAdmin(put("/api/v1/admin/empleados/" + empleado + "/horario"), Map.of("dias", List.of(
                Map.of("diaSemana", fecha.getDayOfWeek().getValue(),
                        "tramos", List.of(Map.of("inicio", "10:00", "fin", "12:00"))))))
                .andExpect(status().isOk());

        consultar(corte, empleado, fecha)
                .andExpect(jsonPath("$.horas.length()").value(4))
                .andExpect(jsonPath("$.horas[0]").value("10:00"))
                .andExpect(jsonPath("$.horas[3]").value("11:30"));
    }

    @Test
    void unDiaLibreOCerradoNoTieneHorasYSugiereElSiguiente() throws Exception {
        enviarAdmin(put("/api/v1/admin/empleados/" + empleado + "/horario"), Map.of("dias", List.of(
                Map.of("diaSemana", fecha.getDayOfWeek().getValue(), "tramos", List.of()))))
                .andExpect(status().isOk());
        consultar(corte, empleado, fecha)
                .andExpect(jsonPath("$.horas").isEmpty())
                .andExpect(jsonPath("$.siguienteFechaConHuecos").value(fecha.plusDays(1).toString()));

        // Cierre del negocio al día siguiente y del empleado al otro.
        cerrar(fecha.plusDays(1), null);
        cerrar(fecha.plusDays(2), empleado);
        consultar(corte, empleado, fecha.plusDays(1))
                .andExpect(jsonPath("$.horas").isEmpty())
                .andExpect(jsonPath("$.siguienteFechaConHuecos").value(fecha.plusDays(3).toString()));
    }

    @Test
    void sinHuecosEnTodoElPlazoNoSugiereNingunDia() throws Exception {
        enviarAdmin(put("/api/v1/admin/empleados/" + empleado + "/horario"), Map.of("dias", IntStream.rangeClosed(1, 7)
                .mapToObj(d -> Map.of("diaSemana", d, "tramos", List.of())).toList()))
                .andExpect(status().isOk());

        consultar(corte, empleado, fecha)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas").isEmpty())
                .andExpect(jsonPath("$.siguienteFechaConHuecos").doesNotExist());
    }

    @Test
    void soloAdmiteFechasDeHoyHastaLaAntelacionMaxima() throws Exception {
        consultar(corte, empleado, hoy.minusDays(1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La fecha ya ha pasado."));
        consultar(corte, empleado, hoy.plusDays(61))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Solo se puede reservar con 60 días de antelación como máximo."));
        consultar(corte, empleado, hoy.plusDays(60)).andExpect(status().isOk());
        consultar(corte, empleado, hoy).andExpect(status().isOk());
    }

    @Test
    void elServicioYElProfesionalTienenQueEstarActivosYCorresponderse() throws Exception {
        long otro = api.crearServicio(admin, ApiDePrueba.nombreUnico("Barba"), 30, "8.00");
        consultar(otro, empleado, fecha)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Ese profesional no realiza este servicio."));
        consultar(999999L, empleado, fecha).andExpect(status().isNotFound());
        consultar(corte, 999999L, fecha).andExpect(status().isNotFound());

        enviarAdmin(patch("/api/v1/admin/servicios/" + tinte + "/activo"), Map.of("activo", false));
        consultar(tinte, empleado, fecha)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El servicio no existe."));

        enviarAdmin(patch("/api/v1/admin/empleados/" + empleado + "/activo"), Map.of("activo", false))
                .andExpect(status().isOk());
        consultar(corte, empleado, fecha)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El profesional no existe."));
    }

    @Test
    void faltanParametrosOTienenMalFormato() throws Exception {
        mockMvc.perform(get("/api/v1/disponibilidad").param("servicioId", String.valueOf(corte))
                        .param("fecha", fecha.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/disponibilidad").param("servicioId", String.valueOf(corte))
                        .param("profesionalId", String.valueOf(empleado)).param("fecha", "03/11/2026"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions consultar(long servicioId, long profesionalId, LocalDate dia) throws Exception {
        return mockMvc.perform(get("/api/v1/disponibilidad")
                .param("servicioId", String.valueOf(servicioId))
                .param("profesionalId", String.valueOf(profesionalId))
                .param("fecha", dia.toString()));
    }

    private void cerrar(LocalDate dia, Long empleadoId) throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("fecha", dia.toString());
        cuerpo.put("empleadoId", empleadoId);
        enviarAdmin(post("/api/v1/admin/dias-cierre"), cuerpo).andExpect(status().isCreated());
    }

    private static Instant instante(LocalDate dia, String hora) {
        return ZonedDateTime.of(dia, LocalTime.parse(hora), MADRID).toInstant();
    }

    private long clienteNuevo() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    private ResultActions enviarAdmin(MockHttpServletRequestBuilder peticion, Object cuerpo) throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(cuerpo)));
    }
}
