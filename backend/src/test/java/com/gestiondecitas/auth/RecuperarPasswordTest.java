package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.CorreosDePrueba;
import com.gestiondecitas.IntegracionTest;

import tools.jackson.databind.json.JsonMapper;

/** CU-020 · Recuperar contraseña. */
@IntegracionTest
class RecuperarPasswordTest {

    private static final String NUEVA = "Recuperada-por-correo-26";

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
    void conUnCorreoRegistradoEnviaUnEnlace() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);

        solicitar(email)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje")
                        .value("Si el correo está registrado, recibirás un enlace en unos minutos."));

        assertThat(correos.para(email)).hasSize(1);
        assertThat(correos.para(email).getFirst().texto())
                .contains("http://localhost:5173/restablecer-password#token=");
        // En la base de datos solo está el hash, no el token del correo.
        String token = correos.ultimoToken(email);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM token_recuperacion WHERE token_hash = ?",
                Integer.class, TokenAleatorio.sha256(token))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM token_recuperacion WHERE token_hash = ?",
                Integer.class, token)).isZero();
    }

    @Test
    void conUnCorreoNoRegistradoRespondeLoMismoYNoEnviaNada() throws Exception {
        String email = ApiDePrueba.emailNuevo();

        solicitar(email)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje")
                        .value("Si el correo está registrado, recibirás un enlace en unos minutos."));

        assertThat(correos.para(email)).isEmpty();
    }

    @Test
    void aUnaCuentaDesactivadaNoLeEnviaNada() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        jdbc.update("UPDATE usuario SET activo = FALSE WHERE email = ?", email);

        solicitar(email).andExpect(status().isAccepted());

        assertThat(correos.para(email)).isEmpty();
    }

    @Test
    void laCuartaSolicitudEnUnaHoraDa429() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        for (int i = 0; i < 3; i++) {
            solicitar(email).andExpect(status().isAccepted());
        }
        solicitar(email)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void conElEnlaceSeEligeUnaContrasenaNuevaYSeCierranLasSesiones() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        var sesionAnterior = api.iniciarSesion(email, ApiDePrueba.PASSWORD);
        solicitar(email);

        restablecer(correos.ultimoToken(email), NUEVA, NUEVA).andExpect(status().isNoContent());

        assertThat(api.login(email, ApiDePrueba.PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(api.login(email, NUEVA).getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(sesionAnterior.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elEnlaceSoloSirveUnaVez() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        solicitar(email);
        String token = correos.ultimoToken(email);

        restablecer(token, NUEVA, NUEVA).andExpect(status().isNoContent());
        restablecer(token, "Otra-clave-distinta-26", "Otra-clave-distinta-26")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("El enlace no es válido o ha caducado. Solicita uno nuevo."));
    }

    @Test
    void pedirOtroEnlaceInvalidaElAnterior() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        solicitar(email);
        String primero = correos.ultimoToken(email);
        solicitar(email);
        String segundo = correos.ultimoToken(email);

        restablecer(primero, NUEVA, NUEVA).andExpect(status().isUnprocessableContent());
        restablecer(segundo, NUEVA, NUEVA).andExpect(status().isNoContent());
    }

    @Test
    void unEnlaceCaducadoNoSirve() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        solicitar(email);
        String token = correos.ultimoToken(email);
        jdbc.update("UPDATE token_recuperacion SET expira_en = now() - interval '1 minute' WHERE token_hash = ?",
                TokenAleatorio.sha256(token));

        restablecer(token, NUEVA, NUEVA).andExpect(status().isUnprocessableContent());
        assertThat(api.login(email, ApiDePrueba.PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unTokenInventadoNoSirve() throws Exception {
        restablecer(TokenAleatorio.generar(), NUEVA, NUEVA).andExpect(status().isUnprocessableContent());
    }

    @Test
    void validaLaNuevaContrasena() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);
        solicitar(email);
        String token = correos.ultimoToken(email);

        restablecer(token, NUEVA, "No-coincide-con-la-otra")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("Las contraseñas no coinciden."));
        restablecer(token, "1234567890", "1234567890").andExpect(status().isBadRequest());
        // Los errores de formato no gastan el enlace.
        restablecer(token, NUEVA, NUEVA).andExpect(status().isNoContent());
    }

    @Test
    void elEmpleadoQueRecuperaLaContrasenaYaNoTieneQueCambiarla() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        ApiDePrueba.crearEmpleado(jdbc, passwordEncoder, email, "Temporal-del-admin-1");
        solicitar(email);

        restablecer(correos.ultimoToken(email), NUEVA, NUEVA).andExpect(status().isNoContent());

        var login = api.login(email, NUEVA);
        assertThat(api.leer(login).get("usuario").get("debeCambiarPassword").asBoolean()).isFalse();
    }

    private ResultActions solicitar(String email) throws Exception {
        return mockMvc.perform(ApiDePrueba.conIpNueva(post("/api/v1/auth/recuperar-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(Map.of("email", email))));
    }

    private ResultActions restablecer(String token, String password, String confirmacion) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/restablecer-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(Map.of("token", token, "password", password,
                        "confirmacionPassword", confirmacion))));
    }
}
