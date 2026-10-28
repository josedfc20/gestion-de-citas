package com.gestiondecitas.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.CorreosDePrueba;
import com.gestiondecitas.IntegracionTest;

import tools.jackson.databind.json.JsonMapper;

/** RNF-007 (RGPD): exportar mis datos y eliminar mi cuenta (CU-004, alt. 3). */
@IntegracionTest
class DatosPersonalesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorreosDePrueba correos;

    private ApiDePrueba api;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
    }

    @Test
    void elClienteExportaSusDatosSinHashes() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        var sesion = api.iniciarSesion(email, ApiDePrueba.PASSWORD);

        String cuerpo = mockMvc.perform(get("/api/v1/me/datos").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"mis-datos.json\""))
                .andExpect(jsonPath("$.generadoEn").isNotEmpty())
                .andExpect(jsonPath("$.cuenta.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.cuenta.email").value(email))
                .andExpect(jsonPath("$.cuenta.apellidos").value("García López"))
                .andExpect(jsonPath("$.cuenta.telefono").value("600123123"))
                .andExpect(jsonPath("$.cliente.fechaNacimiento").value("1990-05-20"))
                .andExpect(jsonPath("$.cliente.privacidadAceptadaEn").isNotEmpty())
                .andExpect(jsonPath("$.sesionesAbiertas.length()").value(1))
                .andExpect(jsonPath("$.beneficiarios").isEmpty())
                .andExpect(jsonPath("$.citas").isEmpty())
                .andReturn().getResponse().getContentAsString();

        String hash = jdbc.queryForObject("SELECT password_hash FROM usuario WHERE email = ?", String.class, email);
        assertThat(cuerpo).doesNotContain(hash).doesNotContain("passwordHash").doesNotContain("tokenHash")
                .doesNotContain(sesion.refreshToken().getValue());
    }

    @Test
    void exportarExigeSesionYContrasenaDefinitiva() throws Exception {
        mockMvc.perform(get("/api/v1/me/datos")).andExpect(status().isUnauthorized());

        String email = ApiDePrueba.emailNuevo();
        ApiDePrueba.crearEmpleado(jdbc, passwordEncoder, email, "Temporal-del-admin-1");
        var temporal = api.iniciarSesion(email, "Temporal-del-admin-1");
        mockMvc.perform(get("/api/v1/me/datos").header(HttpHeaders.AUTHORIZATION, temporal.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void elEmpleadoTambienExportaSusDatos() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        ApiDePrueba.crearEmpleado(jdbc, passwordEncoder, email, "Temporal-del-admin-1");
        jdbc.update("UPDATE empleado SET debe_cambiar_password = FALSE WHERE usuario_id = "
                + "(SELECT id FROM usuario WHERE email = ?)", email);
        var sesion = api.iniciarSesion(email, "Temporal-del-admin-1");

        mockMvc.perform(get("/api/v1/me/datos").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.rol").value("EMPLEADO"))
                .andExpect(jsonPath("$.cliente").doesNotExist());
    }

    @Test
    void eliminarLaCuentaPideLaContrasena() throws Exception {
        var sesion = api.clienteConSesion();

        eliminar(sesion.bearer(), "No-es-mi-clave-2026")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La contraseña no es correcta."));
        eliminar(sesion.bearer(), "").andExpect(status().isBadRequest());
    }

    @Test
    void soloLosClientesEliminanSuCuenta() throws Exception {
        String empleado = ApiDePrueba.emailNuevo();
        ApiDePrueba.crearEmpleado(jdbc, passwordEncoder, empleado, "Temporal-del-admin-1");
        jdbc.update("UPDATE empleado SET debe_cambiar_password = FALSE WHERE usuario_id = "
                + "(SELECT id FROM usuario WHERE email = ?)", empleado);
        var sesionEmpleado = api.iniciarSesion(empleado, "Temporal-del-admin-1");
        eliminar(sesionEmpleado.bearer(), "Temporal-del-admin-1").andExpect(status().isForbidden());

        String admin = ApiDePrueba.emailNuevo();
        jdbc.update("""
                INSERT INTO usuario (rol, nombre, apellidos, email, password_hash, creado_en)
                VALUES ('ADMIN', 'Otro', 'Admin', ?, ?, ?)""",
                admin, passwordEncoder.encode(ApiDePrueba.PASSWORD), java.sql.Timestamp.from(Instant.now()));
        var sesionAdmin = api.iniciarSesion(admin, ApiDePrueba.PASSWORD);
        eliminar(sesionAdmin.bearer(), ApiDePrueba.PASSWORD).andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarLaCuentaAnonimizaLosDatosYCierraLasSesiones() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        var otroDispositivo = api.iniciarSesion(email, ApiDePrueba.PASSWORD);
        var sesion = api.iniciarSesion(email, ApiDePrueba.PASSWORD);
        Long id = jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);

        eliminar(sesion.bearer(), ApiDePrueba.PASSWORD)
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));

        Map<String, Object> fila = jdbc.queryForMap("""
                SELECT u.nombre, u.apellidos, u.email, u.telefono, u.activo, u.eliminado_en,
                       c.fecha_nacimiento, c.privacidad_aceptada_en
                FROM usuario u JOIN cliente c ON c.usuario_id = u.id WHERE u.id = ?""", id);
        assertThat(fila.get("nombre")).isEqualTo("Usuario");
        assertThat(fila.get("apellidos")).isEqualTo("eliminado");
        assertThat(fila.get("email")).isEqualTo("eliminado-" + id + "@anonimo.invalid");
        assertThat(fila.get("telefono")).isNull();
        assertThat(fila.get("activo")).isEqualTo(false);
        assertThat(fila.get("eliminado_en")).isNotNull();
        assertThat(fila.get("fecha_nacimiento")).isNull();
        assertThat(fila.get("privacidad_aceptada_en")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE usuario_id = ?", Integer.class, id))
                .isZero();

        assertThat(api.login(email, ApiDePrueba.PASSWORD).getResponse().getStatus()).isEqualTo(401);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(otroDispositivo.refreshToken()))
                .andExpect(status().isUnauthorized());

        // Pedir un enlace con el correo original no envía nada: esa cuenta ya no existe.
        mockMvc.perform(ApiDePrueba.conIpNueva(post("/api/v1/auth/recuperar-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.aJson(Map.of("email", email))))
                .andExpect(status().isAccepted());
        assertThat(correos.para(email)).isEmpty();

        // El correo queda libre para registrarse de nuevo.
        api.registrar(email);
    }

    private ResultActions eliminar(String bearer, String password) throws Exception {
        return mockMvc.perform(delete("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(Map.of("password", password))));
    }
}
