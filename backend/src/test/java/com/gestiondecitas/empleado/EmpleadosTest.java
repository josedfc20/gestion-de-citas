package com.gestiondecitas.empleado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** CU-017 (gestionar empleados), CU-006 (profesionales) y CU-021 (servicios del empleado). */
@IntegracionTest
class EmpleadosTest {

    private static final String DEFINITIVA = "Mi-clave-de-empleado-2026";

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
    void elAltaDevuelveUnaContrasenaTemporalQueSoloSirveParaCambiarla() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        String email = ApiDePrueba.emailNuevo();

        JsonNode creado = api.leer(alta("Luis", "García López", email, List.of(corte))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empleado.email").value(email))
                .andExpect(jsonPath("$.empleado.activo").value(true))
                .andExpect(jsonPath("$.empleado.debeCambiarPassword").value(true))
                .andExpect(jsonPath("$.empleado.servicios[0].id").value(corte))
                .andExpect(jsonPath("$.passwordTemporal").isString())
                .andReturn());
        String temporal = creado.get("passwordTemporal").asString();
        assertThat(temporal).hasSize(12);
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE email = ?", String.class, email);
        assertThat(hash).startsWith("$2").doesNotContain(temporal);

        var sesion = api.iniciarSesion(email, temporal);
        mockMvc.perform(get("/api/v1/empleado/servicios").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isForbidden());

        MvcResult cambio = mockMvc.perform(put("/api/v1/me/password")
                        .header(HttpHeaders.AUTHORIZATION, sesion.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.aJson(Map.of("passwordActual", temporal, "passwordNueva", DEFINITIVA,
                                "confirmacionPassword", DEFINITIVA))))
                .andExpect(status().isOk())
                .andReturn();
        String definitivo = "Bearer " + api.leer(cambio).get("accessToken").asString();
        mockMvc.perform(get("/api/v1/empleado/servicios").header(HttpHeaders.AUTHORIZATION, definitivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(corte))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[0].precio").value(15.00));
    }

    @Test
    void noSeDaDeAltaConUnCorreoEnUsoNiConServiciosNoValidos() throws Exception {
        String emailCliente = ApiDePrueba.emailNuevo();
        api.registrar(emailCliente);
        alta("Ana", "Ruiz", emailCliente.toUpperCase(), List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese correo ya lo usa otra cuenta."));

        long inactivo = api.crearServicio(admin, ApiDePrueba.nombreUnico("Viejo"), 30, "5.00");
        enviarAdmin(patch("/api/v1/admin/servicios/" + inactivo + "/activo"), Map.of("activo", false));
        alta("Ana", "Ruiz", ApiDePrueba.emailNuevo(), List.of(inactivo, 999999L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "Estos servicios no existen o están desactivados: [" + inactivo + ", 999999]."));
    }

    @Test
    void elAdminEditaLosDatosYLosServicios() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long tinte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Tinte"), 60, "40.00");
        long id = idDe(alta("Eva", "Martín", ApiDePrueba.emailNuevo(), List.of(corte)));
        String otroEmail = ApiDePrueba.emailNuevo();

        enviarAdmin(put("/api/v1/admin/empleados/" + id),
                Map.of("nombre", "Eva María", "apellidos", "Martín Sanz", "email", otroEmail, "telefono", "611222333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Eva María"))
                .andExpect(jsonPath("$.email").value(otroEmail))
                .andExpect(jsonPath("$.telefono").value("611222333"));

        enviarAdmin(put("/api/v1/admin/empleados/" + id + "/servicios"), Map.of("servicioIds", List.of(corte, tinte)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios[*].id",
                        containsInAnyOrder((int) corte, (int) tinte)))
                .andExpect(jsonPath("$.avisos").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/empleados/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.servicios.length()").value(2));
    }

    @Test
    void quitarUnServicioConCitasFuturasAvisaPeroLasMantiene() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long id = idDe(alta("Raúl", "Gil", ApiDePrueba.emailNuevo(), List.of(corte)));
        long cita = ApiDePrueba.crearCita(jdbc, clienteNuevo(), id, corte, Instant.now().plus(Duration.ofDays(3)));

        enviarAdmin(put("/api/v1/admin/empleados/" + id + "/servicios"), Map.of("servicioIds", List.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios").isEmpty())
                .andExpect(jsonPath("$.avisos[0]").value(org.hamcrest.Matchers.startsWith("Tiene 1 cita futura de «")));
        assertThat(jdbc.queryForObject("SELECT estado FROM cita WHERE id = ?", String.class, cita))
                .isEqualTo("CONFIRMADA");
    }

    @Test
    void unServicioDesactivadoYaAsignadoPuedeSeguirEnLaLista() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long id = idDe(alta("Iris", "Paz", ApiDePrueba.emailNuevo(), List.of(corte)));
        enviarAdmin(patch("/api/v1/admin/servicios/" + corte + "/activo"), Map.of("activo", false));

        enviarAdmin(put("/api/v1/admin/empleados/" + id + "/servicios"), Map.of("servicioIds", List.of(corte)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios[0].id").value(corte));
    }

    @Test
    void noSeDesactivaAQuienTieneCitasFuturas() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long id = idDe(alta("Pablo", "Ríos", ApiDePrueba.emailNuevo(), List.of(corte)));
        long cliente = clienteNuevo();
        ApiDePrueba.crearCita(jdbc, cliente, id, corte, Instant.now().plus(Duration.ofDays(1)));
        ApiDePrueba.crearCita(jdbc, cliente, id, corte, Instant.now().plus(Duration.ofDays(2)));
        // Una cita pasada no cuenta.
        ApiDePrueba.crearCita(jdbc, cliente, id, corte, Instant.now().minus(Duration.ofDays(2)));

        enviarAdmin(patch("/api/v1/admin/empleados/" + id + "/activo"), Map.of("activo", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.citasFuturas").value(2))
                .andExpect(jsonPath("$.detail").value(
                        "Tiene 2 citas futuras. Cancélalas o reasígnalas antes de desactivarlo."));
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void desactivarCierraSusSesionesYLoOcultaDelPublico() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        String email = ApiDePrueba.emailNuevo();
        JsonNode creado = api.leer(alta("Sara", "Luna", email, List.of(corte)).andReturn());
        long id = creado.get("empleado").get("id").asLong();
        var sesion = api.iniciarSesion(email, creado.get("passwordTemporal").asString());

        enviarAdmin(patch("/api/v1/admin/empleados/" + id + "/activo"), Map.of("activo", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(sesion.refreshToken()))
                .andExpect(status().isUnauthorized());
        assertThat(api.login(email, creado.get("passwordTemporal").asString()).getResponse().getStatus())
                .isEqualTo(403);
        mockMvc.perform(get("/api/v1/profesionales"))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));

        enviarAdmin(patch("/api/v1/admin/empleados/" + id + "/activo"), Map.of("activo", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.servicios[0].id").value(corte));
        mockMvc.perform(get("/api/v1/profesionales"))
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }

    @Test
    void unaNuevaContrasenaTemporalInvalidaLaAnterior() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        JsonNode creado = api.leer(alta("Teo", "Vidal", email, List.of()).andReturn());
        long id = creado.get("empleado").get("id").asLong();
        String primera = creado.get("passwordTemporal").asString();

        JsonNode nueva = api.leer(enviarAdmin(post("/api/v1/admin/empleados/" + id + "/password-temporal"), Map.of())
                .andExpect(status().isOk())
                .andReturn());
        String segunda = nueva.get("passwordTemporal").asString();

        assertThat(segunda).isNotEqualTo(primera);
        assertThat(api.login(email, primera).getResponse().getStatus()).isEqualTo(401);
        assertThat(api.leer(api.login(email, segunda)).get("usuario").get("debeCambiarPassword").asBoolean())
                .isTrue();
    }

    @Test
    void lasRutasDeEmpleadosNoTocanClientesNiAlAdmin() throws Exception {
        long cliente = clienteNuevo();
        mockMvc.perform(get("/api/v1/admin/empleados/" + cliente).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        enviarAdmin(patch("/api/v1/admin/empleados/" + cliente + "/activo"), Map.of("activo", false))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/empleados").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem((int) cliente))));
    }

    @Test
    void losProfesionalesPublicosSoloMuestranNombreEInicialYSeFiltranPorServicio() throws Exception {
        long corte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Corte"), 30, "15.00");
        long tinte = api.crearServicio(admin, ApiDePrueba.nombreUnico("Tinte"), 60, "40.00");
        long luis = idDe(alta("Luis", "García López", ApiDePrueba.emailNuevo(), List.of(corte, tinte)));
        long marta = idDe(alta("Marta", "Soler", ApiDePrueba.emailNuevo(), List.of(tinte)));
        enviarAdmin(patch("/api/v1/admin/servicios/" + tinte + "/activo"), Map.of("activo", false));

        mockMvc.perform(get("/api/v1/profesionales").param("servicioId", String.valueOf(corte)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(luis))
                .andExpect(jsonPath("$[0].nombre").value("Luis G."))
                .andExpect(jsonPath("$[0].servicios.length()").value(1)) // el tinte está desactivado
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].telefono").doesNotExist())
                .andExpect(jsonPath("$[0].apellidos").doesNotExist());

        mockMvc.perform(get("/api/v1/profesionales"))
                .andExpect(jsonPath("$[*].id", hasItem((int) marta)));
        mockMvc.perform(get("/api/v1/profesionales").param("servicioId", String.valueOf(tinte)))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloElAdminGestionaEmpleadosYSoloElEmpleadoVeSusServicios() throws Exception {
        var cliente = api.clienteConSesion();
        mockMvc.perform(get("/api/v1/admin/empleados").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/empleado/servicios").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/empleados")).andExpect(status().isUnauthorized());
    }

    private ResultActions alta(String nombre, String apellidos, String email, List<Long> servicioIds)
            throws Exception {
        return enviarAdmin(post("/api/v1/admin/empleados"), Map.of("nombre", nombre, "apellidos", apellidos,
                "email", email, "servicioIds", servicioIds));
    }

    private long idDe(ResultActions alta) throws Exception {
        return api.leer(alta.andExpect(status().isCreated()).andReturn()).get("empleado").get("id").asLong();
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
