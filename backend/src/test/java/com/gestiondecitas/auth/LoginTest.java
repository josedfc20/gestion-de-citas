package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;

import tools.jackson.databind.json.JsonMapper;

/** CU-002 · Iniciar sesión. */
@IntegracionTest
class LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiDePrueba api;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
    }

    @Test
    void devuelveElAccessTokenYLaCookieDelRefreshToken() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);

        MvcResult resultado = api.login(email.toUpperCase(), ApiDePrueba.PASSWORD);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        var cuerpo = api.leer(resultado);
        assertThat(cuerpo.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(cuerpo.get("expiraEnSegundos").asLong()).isEqualTo(900);
        assertThat(cuerpo.get("usuario").get("rol").asString()).isEqualTo("CLIENTE");

        String cookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("refresh_token=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=604800");

        // El access token sirve para las rutas con sesión.
        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cuerpo.get("accessToken").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void contrasenaIncorrectaYCorreoInexistenteDanElMismoMensaje() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);

        MvcResult malaPassword = api.login(email, "No-es-la-clave-123");
        MvcResult correoInexistente = api.login(ApiDePrueba.emailNuevo(), ApiDePrueba.PASSWORD);

        for (MvcResult resultado : new MvcResult[] {malaPassword, correoInexistente}) {
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(api.leer(resultado).get("detail").asString()).isEqualTo("Correo o contraseña no válidos.");
            assertThat(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        }
    }

    @Test
    void cuentaDesactivadaDevuelve403SoloConLaContrasenaCorrecta() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        jdbc.update("UPDATE usuario SET activo = FALSE WHERE email = ?", email);

        assertThat(api.login(email, "No-es-la-clave-123").getResponse().getStatus()).isEqualTo(401);

        MvcResult resultado = api.login(email, ApiDePrueba.PASSWORD);
        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(api.leer(resultado).get("detail").asString())
                .isEqualTo("Tu cuenta está desactivada. Ponte en contacto con el negocio.");
    }

    @Test
    void elSextoIntentoEnUnMinutoDesdeLaMismaIpDevuelve429() throws Exception {
        String cuerpo = api.aJson(Map.of("email", ApiDePrueba.emailNuevo(), "password", "No-es-la-clave-123"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login").with(ip("192.0.2.10"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login").with(ip("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("Demasiados intentos"));
    }

    @Test
    void elLimitePorCorreoFuncionaAunqueCambieLaIp() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        for (int i = 0; i < 5; i++) {
            api.login(email, "No-es-la-clave-123");
        }

        // Ni siquiera con la contraseña correcta: hay que esperar.
        assertThat(api.login(email, ApiDePrueba.PASSWORD).getResponse().getStatus()).isEqualTo(429);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String ip) {
        return peticion -> {
            peticion.setRemoteAddr(ip);
            return peticion;
        };
    }
}
