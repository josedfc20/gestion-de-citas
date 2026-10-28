package com.gestiondecitas.negocio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** Configuración del negocio ({@code /admin/negocio}) y RN-011 al cambiar el intervalo. */
@IntegracionTest
class NegocioTest {

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
    private Map<String, Object> original;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
        admin = ApiDePrueba.bearerAdmin(jwtService, jdbc);
        original = jdbc.queryForMap("SELECT * FROM negocio ORDER BY id LIMIT 1");
    }

    /** El negocio es compartido por todas las pruebas: se deja como estaba. */
    @AfterEach
    void restaurar() {
        jdbc.update("""
                UPDATE negocio SET nombre = ?, zona_horaria = ?, intervalo_minutos = ?, antelacion_max_dias = ?,
                horas_min_cancelacion = ?, edad_minima_cuenta = ? WHERE id = ?""",
                original.get("nombre"), original.get("zona_horaria"), original.get("intervalo_minutos"),
                original.get("antelacion_max_dias"), original.get("horas_min_cancelacion"),
                original.get("edad_minima_cuenta"), original.get("id"));
    }

    @Test
    void elAdminVeYCambiaLaConfiguracion() throws Exception {
        mockMvc.perform(get("/api/v1/admin/negocio").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervaloMinutos").value(30))
                .andExpect(jsonPath("$.zonaHoraria").value("Europe/Madrid"));

        Map<String, Object> datos = datos(15);
        datos.put("nombre", "  Peluquería Nueva ");
        guardar(datos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Peluquería Nueva"))
                .andExpect(jsonPath("$.intervaloMinutos").value(15))
                .andExpect(jsonPath("$.antelacionMaxDias").value(90))
                .andExpect(jsonPath("$.horasMinCancelacion").value(12))
                .andExpect(jsonPath("$.edadMinimaCuenta").value(16));
    }

    @Test
    void noSeCambiaElIntervaloSiAlgunServicioDejariaDeSerMultiplo() throws Exception {
        String nombre = ApiDePrueba.nombreUnico("Corte");
        api.crearServicio(admin, nombre, 30, "10.00");

        guardar(datos(60))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail", containsString("no son múltiplo de 60 minutos")))
                .andExpect(jsonPath("$.detail", containsString(nombre + " (30)")));
    }

    @Test
    void valoresFueraDeRangoSonErroresDeFormato() throws Exception {
        guardar(datos(25))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("intervaloMinutosValido"));

        Map<String, Object> menor = datos(30);
        menor.put("edadMinimaCuenta", 13);
        guardar(menor)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("edadMinimaCuenta"));

        Map<String, Object> zona = datos(30);
        zona.put("zonaHoraria", "Marte/Olympus");
        guardar(zona)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("zonaHorariaValida"));
    }

    @Test
    void soloElAdmin() throws Exception {
        var cliente = api.clienteConSesion();
        mockMvc.perform(get("/api/v1/admin/negocio").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isForbidden());
    }

    private ResultActions guardar(Map<String, Object> datos) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/negocio")
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(datos)));
    }

    private static Map<String, Object> datos(int intervalo) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Peluquería de pruebas");
        datos.put("zonaHoraria", "Europe/Madrid");
        datos.put("intervaloMinutos", intervalo);
        datos.put("antelacionMaxDias", 90);
        datos.put("horasMinCancelacion", 12);
        datos.put("edadMinimaCuenta", 16);
        return datos;
    }
}
