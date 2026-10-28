package com.gestiondecitas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

/** Rutas públicas, rutas por rol (paso 3.4) y tokens no válidos. */
@IntegracionTest
class SeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    private ApiDePrueba api;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void laDocumentacionOpenApiEsPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("GestionDeCitas API"));
    }

    @Test
    void sinTokenLaApiPideSesionConProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/v1/me"));
    }

    @Test
    void lasConsultasDelCatalogoSonPublicas() throws Exception {
        // Aún no existen (B4): sin seguridad, la respuesta es 404 y no 401.
        mockMvc.perform(get("/api/v1/servicios"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unClienteNoEntraEnLasRutasDeAdministradorNiDeEmpleado() throws Exception {
        var sesion = api.clienteConSesion();

        mockMvc.perform(get("/api/v1/admin/servicios").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tienes permiso para esta operación."));
        mockMvc.perform(get("/api/v1/empleado/agenda").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorEntraEnSusRutas() throws Exception {
        var sesion = api.iniciarSesion("admin@pruebas.local", "Admin-de-pruebas-2026");

        // Pasa la seguridad; el endpoint aún no existe (B4).
        mockMvc.perform(get("/api/v1/admin/servicios").header(HttpHeaders.AUTHORIZATION, sesion.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unTokenManipuladoDevuelve401() throws Exception {
        var sesion = api.clienteConSesion();
        String token = sesion.accessToken();
        String manipulado = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulado))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unTokenCaducadoDevuelve401() throws Exception {
        Instant haceUnaHora = Instant.now().minus(1, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(haceUnaHora)
                .expiresAt(haceUnaHora.plus(15, ChronoUnit.MINUTES))
                .claim("rol", "ADMIN")
                .claim("dcp", false)
                .build();
        String caducado = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + caducado))
                .andExpect(status().isUnauthorized());
    }
}
