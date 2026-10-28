package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;

import tools.jackson.databind.json.JsonMapper;

/** Renovar la sesión con rotación del refresh token y cerrar sesión (CU-003). */
@IntegracionTest
class SesionTest {

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
    void elRefreshDaUnAccessTokenNuevoYCambiaElRefreshToken() throws Exception {
        var sesion = api.clienteConSesion();

        MvcResult resultado = refresh(sesion.refreshToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie nueva = resultado.getResponse().getCookie("refresh_token");
        assertThat(nueva.getValue()).isNotBlank().isNotEqualTo(sesion.refreshToken().getValue());
        refresh(nueva).andExpect(status().isOk());
    }

    @Test
    void reutilizarUnRefreshTokenYaUsadoCierraTodasLasSesiones() throws Exception {
        var sesion = api.clienteConSesion();
        Cookie nueva = refresh(sesion.refreshToken()).andReturn().getResponse().getCookie("refresh_token");

        // Alguien usa el token viejo (robado): 401 y se revocan todos, también el nuevo.
        refresh(sesion.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge("refresh_token", 0));
        refresh(nueva).andExpect(status().isUnauthorized());
    }

    @Test
    void sinCookieOConUnTokenInventadoDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("La sesión ha caducado. Vuelve a iniciar sesión."));
        refresh(new Cookie("refresh_token", "inventado")).andExpect(status().isUnauthorized());
    }

    @Test
    void unRefreshTokenCaducadoDevuelve401() throws Exception {
        var sesion = api.clienteConSesion();
        jdbc.update("UPDATE refresh_token SET expira_en = now() - interval '1 minute' WHERE token_hash = ?",
                TokensDeSesion.hash(sesion.refreshToken().getValue()));

        refresh(sesion.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void elLogoutRevocaElRefreshTokenYBorraLaCookie() throws Exception {
        var sesion = api.clienteConSesion();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(sesion.refreshToken()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));

        refresh(sesion.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void elLogoutSinSesionTambienTerminaBien() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void enLaBaseDeDatosSoloSeGuardaElHashDelRefreshToken() throws Exception {
        var sesion = api.clienteConSesion();
        String valor = sesion.refreshToken().getValue();

        Integer enClaro = jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE token_hash = ?",
                Integer.class, valor);
        Integer conHash = jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE token_hash = ?",
                Integer.class, TokensDeSesion.hash(valor));
        assertThat(enClaro).isZero();
        assertThat(conHash).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie));
    }
}
