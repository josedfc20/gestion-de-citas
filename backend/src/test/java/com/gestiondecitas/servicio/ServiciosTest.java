package com.gestiondecitas.servicio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

/** CU-016 (gestionar servicios) y CU-005 (catálogo público). */
@IntegracionTest
class ServiciosTest {

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
    void elAdminCreaUnServicioYSaleEnElCatalogoPublico() throws Exception {
        String nombre = ApiDePrueba.nombreUnico("Corte");
        enviar(post("/api/v1/admin/servicios"), datos(nombre, 30, "15.50"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/admin/servicios/")))
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.precio").value(15.50))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.empleados").value(0));

        mockMvc.perform(get("/api/v1/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem(nombre)))
                .andExpect(jsonPath("$[0].activo").doesNotExist());
    }

    @Test
    void elNombreNoSePuedeRepetirAunqueCambienLasMayusculas() throws Exception {
        String nombre = ApiDePrueba.nombreUnico("Tinte");
        api.crearServicio(admin, nombre, 60, "30.00");

        enviar(post("/api/v1/admin/servicios"), datos("  " + nombre.toUpperCase() + " ", 60, "30.00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe un servicio con ese nombre."));
    }

    @Test
    void laDuracionDebeSerMultiploDelIntervalo() throws Exception {
        enviar(post("/api/v1/admin/servicios"), datos(ApiDePrueba.nombreUnico("Raro"), 45, "10.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "La duración debe ser múltiplo de 30 minutos, el intervalo de reserva del negocio."));
    }

    @Test
    void precioNegativoOConTresDecimalesEsUnErrorDeFormato() throws Exception {
        enviar(post("/api/v1/admin/servicios"), datos(ApiDePrueba.nombreUnico("Negativo"), 30, "-1.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("precio"));
        enviar(post("/api/v1/admin/servicios"), datos(ApiDePrueba.nombreUnico("Decimales"), 30, "10.005"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("precio"));
    }

    @Test
    void elAdminEditaUnServicio() throws Exception {
        long id = api.crearServicio(admin, ApiDePrueba.nombreUnico("Peinado"), 30, "12.00");
        String nuevo = ApiDePrueba.nombreUnico("Peinado largo");
        Map<String, Object> cambios = datos(nuevo, 60, "20.00");
        cambios.put("descripcion", "  Con secado  ");

        enviar(put("/api/v1/admin/servicios/" + id), cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nuevo))
                .andExpect(jsonPath("$.descripcion").value("Con secado"))
                .andExpect(jsonPath("$.duracionMinutos").value(60))
                .andExpect(jsonPath("$.precio").value(20.00));
        enviar(put("/api/v1/admin/servicios/999999"), cambios).andExpect(status().isNotFound());
    }

    @Test
    void desactivarLoOcultaDelPublicoPeroNoDelAdmin() throws Exception {
        String nombre = ApiDePrueba.nombreUnico("Barba");
        long id = api.crearServicio(admin, nombre, 30, "8.00");

        enviar(patch("/api/v1/admin/servicios/" + id + "/activo"), Map.of("activo", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/v1/servicios")).andExpect(jsonPath("$[*].nombre", not(hasItem(nombre))));
        mockMvc.perform(get("/api/v1/servicios/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/servicios").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$[*].nombre", hasItem(nombre)));

        enviar(patch("/api/v1/admin/servicios/" + id + "/activo"), Map.of("activo", true))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/servicios/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nombre));
    }

    @Test
    void soloElAdminGestionaServicios() throws Exception {
        var cliente = api.clienteConSesion();
        mockMvc.perform(get("/api/v1/admin/servicios").header(HttpHeaders.AUTHORIZATION, cliente.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(api.aJson(datos("Sin sesión", 30, "1.00"))))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
            Object cuerpo) throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(cuerpo)));
    }

    private static Map<String, Object> datos(String nombre, int duracion, String precio) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("duracionMinutos", duracion);
        datos.put("precio", new BigDecimal(precio));
        return datos;
    }
}
