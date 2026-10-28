package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;

import tools.jackson.databind.json.JsonMapper;

/** CU-004 · Modificar perfil y contraseña, y la contraseña temporal del empleado (RN-005). */
@IntegracionTest
class PerfilYPasswordTest {

    private static final String NUEVA = "Mi-clave-nueva-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ApiDePrueba api;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
    }

    @Test
    void elClienteVeSusDatos() throws Exception {
        var sesion = api.clienteConSesion();

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.apellidos").value("García López"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1990-05-20"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void elClienteModificaSusDatos() throws Exception {
        var sesion = api.clienteConSesion();
        String nuevoEmail = ApiDePrueba.emailNuevo();

        actualizarPerfil(sesion, nuevoEmail)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"))
                .andExpect(jsonPath("$.email").value(nuevoEmail))
                .andExpect(jsonPath("$.telefono").doesNotExist());
    }

    @Test
    void noPuedeUsarElCorreoDeOtraCuenta() throws Exception {
        String ajeno = ApiDePrueba.emailNuevo();
        api.registrar(ajeno);
        var sesion = api.clienteConSesion();

        actualizarPerfil(sesion, ajeno)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese correo ya lo usa otra cuenta."));
    }

    @Test
    void cambiarLaContrasenaPideLaActualYCierraLasDemasSesiones() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        var otroDispositivo = api.iniciarSesion(email, ApiDePrueba.PASSWORD);
        var sesion = api.iniciarSesion(email, ApiDePrueba.PASSWORD);

        cambiarPassword(sesion.bearer(), "No-es-la-clave-123", NUEVA)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La contraseña actual no es correcta."));

        cambiarPassword(sesion.bearer(), ApiDePrueba.PASSWORD, NUEVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(otroDispositivo.refreshToken()))
                .andExpect(status().isUnauthorized());
        assertThat(api.login(email, ApiDePrueba.PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(api.login(email, NUEVA).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void elEmpleadoConContrasenaTemporalSoloPuedeCambiarla() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        ApiDePrueba.crearEmpleado(jdbc, passwordEncoder, email, "Temporal-del-admin-1");

        MvcResult login = api.login(email, "Temporal-del-admin-1");
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        assertThat(api.leer(login).get("usuario").get("debeCambiarPassword").asBoolean()).isTrue();
        String temporal = "Bearer " + api.leer(login).get("accessToken").asString();

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, temporal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));
        mockMvc.perform(get("/api/v1/empleado/agenda").header(HttpHeaders.AUTHORIZATION, temporal))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Debes cambiar tu contraseña antes de continuar."));
        actualizarPerfil(new ApiDePrueba.Sesion(temporal.substring(7), null), email)
                .andExpect(status().isForbidden());

        MvcResult cambio = cambiarPassword(temporal, "Temporal-del-admin-1", NUEVA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.debeCambiarPassword").value(false))
                .andReturn();
        String definitivo = "Bearer " + api.leer(cambio).get("accessToken").asString();

        // Ya pasa la seguridad y ve su agenda (B6).
        mockMvc.perform(get("/api/v1/empleado/agenda").header(HttpHeaders.AUTHORIZATION, definitivo))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("""
                SELECT e.debe_cambiar_password FROM empleado e JOIN usuario u ON u.id = e.usuario_id
                WHERE u.email = ?""", Boolean.class, email)).isFalse();
    }

    private org.springframework.test.web.servlet.ResultActions actualizarPerfil(ApiDePrueba.Sesion sesion,
            String email) throws Exception {
        return mockMvc.perform(put("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, sesion.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(Map.of("nombre", "Ana María", "apellidos", "García", "email", email))));
    }

    private org.springframework.test.web.servlet.ResultActions cambiarPassword(String bearer, String actual,
            String nueva) throws Exception {
        return mockMvc.perform(put("/api/v1/me/password")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(Map.of("passwordActual", actual, "passwordNueva", nueva,
                        "confirmacionPassword", nueva))));
    }
}
