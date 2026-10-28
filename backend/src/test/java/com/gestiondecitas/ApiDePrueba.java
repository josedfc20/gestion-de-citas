package com.gestiondecitas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.Cookie;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Atajos para las pruebas de integración de la autenticación: registrar,
 * iniciar sesión y crear usuarios directamente en la base de datos. Cada
 * prueba usa correos e IP distintos para no chocar con el límite de intentos.
 */
public final class ApiDePrueba {

    public static final String PASSWORD = "Una-clave-larga-2026";
    private static final AtomicInteger contadorIp = new AtomicInteger(1);

    private final MockMvc mockMvc;
    private final JsonMapper json;

    public ApiDePrueba(MockMvc mockMvc, JsonMapper json) {
        this.mockMvc = mockMvc;
        this.json = json;
    }

    public static String emailNuevo() {
        return "u-" + UUID.randomUUID() + "@pruebas.local";
    }

    /** IP distinta en cada llamada (10.x.y.z), para que el límite por IP no afecte a otras pruebas. */
    public static MockHttpServletRequestBuilder conIpNueva(MockHttpServletRequestBuilder peticion) {
        int n = contadorIp.getAndIncrement();
        String ip = "10." + ((n >> 16) & 255) + "." + ((n >> 8) & 255) + "." + (n & 255);
        return peticion.with(p -> {
            p.setRemoteAddr(ip);
            return p;
        });
    }

    public static Map<String, Object> datosRegistro(String email) {
        return Map.of(
                "nombre", "Ana",
                "apellidos", "García López",
                "email", email,
                "telefono", "600123123",
                "fechaNacimiento", "1990-05-20",
                "password", PASSWORD,
                "confirmacionPassword", PASSWORD,
                "aceptaPrivacidad", true);
    }

    public String aJson(Object cuerpo) {
        return json.writeValueAsString(cuerpo);
    }

    public JsonNode leer(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    public void registrar(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(datosRegistro(email))))
                .andExpect(status().isCreated());
    }

    public MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(conIpNueva(post("/api/v1/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(Map.of("email", email, "password", password))))
                .andReturn();
    }

    /** Inicia sesión y devuelve el access token y la cookie del refresh token. */
    public Sesion iniciarSesion(String email, String password) throws Exception {
        MvcResult resultado = login(email, password);
        if (resultado.getResponse().getStatus() != 200) {
            throw new AssertionError("Login fallido: " + resultado.getResponse().getContentAsString());
        }
        return new Sesion(leer(resultado).get("accessToken").asString(),
                resultado.getResponse().getCookie("refresh_token"));
    }

    /** Registra un cliente nuevo y abre sesión con él. */
    public Sesion clienteConSesion() throws Exception {
        String email = emailNuevo();
        registrar(email);
        return iniciarSesion(email, PASSWORD);
    }

    /** Inserta un empleado con contraseña temporal (la gestión de empleados llega en B4). */
    public static long crearEmpleado(JdbcTemplate jdbc, PasswordEncoder encoder, String email, String password) {
        Long id = jdbc.queryForObject("""
                INSERT INTO usuario (rol, nombre, apellidos, email, password_hash, creado_en)
                VALUES ('EMPLEADO', 'Luis', 'Pérez', ?, ?, ?) RETURNING id""",
                Long.class, email, encoder.encode(password), java.sql.Timestamp.from(Instant.now()));
        jdbc.update("INSERT INTO empleado (usuario_id, debe_cambiar_password) VALUES (?, TRUE)", id);
        return id;
    }

    public record Sesion(String accessToken, Cookie refreshToken) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
