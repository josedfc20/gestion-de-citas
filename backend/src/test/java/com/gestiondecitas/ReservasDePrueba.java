package com.gestiondecitas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/**
 * Escenario de las pruebas de citas (B6): horario general de 09:00 a 14:00 y de
 * 16:00 a 20:00 todos los días, un corte de 30 min, un tinte de 60 min y un
 * profesional que hace los dos. {@code fecha} es dentro de tres días.
 */
public final class ReservasDePrueba {

    public static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private final MockMvc mockMvc;
    private final ApiDePrueba api;
    private final JdbcTemplate jdbc;
    private final JwtService jwtService;
    public final String admin;
    public final long corte;
    public final long tinte;
    public final long empleado;
    public final LocalDate hoy;
    public final LocalDate fecha;

    public ReservasDePrueba(MockMvc mockMvc, JsonMapper jsonMapper, JdbcTemplate jdbc, JwtService jwtService)
            throws Exception {
        this.mockMvc = mockMvc;
        this.api = new ApiDePrueba(mockMvc, jsonMapper);
        this.jdbc = jdbc;
        this.jwtService = jwtService;
        this.admin = ApiDePrueba.bearerAdmin(jwtService, jdbc);
        var manana = Map.of("inicio", "09:00", "fin", "14:00");
        var tarde = Map.of("inicio", "16:00", "fin", "20:00");
        enviar(put("/api/v1/admin/horario-general"), admin, Map.of("dias", IntStream.rangeClosed(1, 7)
                .mapToObj(d -> Map.of("diaSemana", d, "tramos", List.of(manana, tarde))).toList()))
                .andExpect(status().isOk());
        this.corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        this.tinte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Tinte"), 60, "40.00");
        this.empleado = otroEmpleado();
        this.hoy = LocalDate.now(MADRID);
        this.fecha = hoy.plusDays(3);
    }

    public ApiDePrueba api() {
        return api;
    }

    /** Otro profesional ("Luis García") que hace el corte y el tinte. */
    public long otroEmpleado() throws Exception {
        return api.crearEmpleadoConServicios(admin, List.of(corte, tinte));
    }

    /** Access token de un empleado con la contraseña ya cambiada, sin pasar por el login. */
    public String bearerEmpleado(long empleadoId) {
        return "Bearer " + jwtService.emitir(empleadoId, "EMPLEADO", false);
    }

    /** Un cliente nuevo con sesión y su id. */
    public Cliente cliente() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        long id = jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
        return new Cliente(id, email, api.iniciarSesion(email, ApiDePrueba.PASSWORD).bearer());
    }

    public static String inicio(LocalDate dia, String hora) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.of(dia, LocalTime.parse(hora), MADRID));
    }

    public ResultActions reservar(String bearer, long servicioId, long profesionalId, LocalDate dia, String hora,
            Long beneficiarioId) throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("servicioId", servicioId);
        cuerpo.put("profesionalId", profesionalId);
        cuerpo.put("inicio", inicio(dia, hora));
        cuerpo.put("beneficiarioId", beneficiarioId);
        return enviar(post("/api/v1/citas"), bearer, cuerpo);
    }

    /** Reserva un corte con el profesional del escenario y devuelve el id de la cita. */
    public long reservarCorte(Cliente cliente, String hora) throws Exception {
        return idDe(reservar(cliente.bearer(), corte, empleado, fecha, hora, null).andExpect(status().isCreated()));
    }

    public long crearBeneficiario(String bearer, String nombre, LocalDate nacimiento) throws Exception {
        return idDe(enviar(post("/api/v1/me/beneficiarios"), bearer,
                Map.of("nombre", nombre, "apellidos", "García", "fechaNacimiento", nacimiento.toString()))
                .andExpect(status().isCreated()));
    }

    public ResultActions disponibilidad(long servicioId, long profesionalId, LocalDate dia) throws Exception {
        return mockMvc.perform(get("/api/v1/disponibilidad")
                .param("servicioId", String.valueOf(servicioId))
                .param("profesionalId", String.valueOf(profesionalId))
                .param("fecha", dia.toString()));
    }

    public ResultActions enviar(MockHttpServletRequestBuilder peticion, String bearer, Object cuerpo)
            throws Exception {
        if (bearer != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        return mockMvc.perform(peticion.contentType(MediaType.APPLICATION_JSON).content(api.aJson(cuerpo)));
    }

    public ResultActions leer(String ruta, String bearer) throws Exception {
        return mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private long idDe(ResultActions resultado) throws Exception {
        return api.leer(resultado.andReturn()).get("id").asLong();
    }

    public record Cliente(long id, String email, String bearer) {
    }
}
