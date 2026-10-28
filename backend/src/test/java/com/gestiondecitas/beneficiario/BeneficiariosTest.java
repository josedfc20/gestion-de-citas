package com.gestiondecitas.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.ReservasDePrueba;
import com.gestiondecitas.ReservasDePrueba.Cliente;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-009: menores a cargo de un cliente (RN-007, RNF-007). */
@IntegracionTest
class BeneficiariosTest {

    private static final String RUTA = "/api/v1/me/beneficiarios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private ReservasDePrueba r;
    private Cliente cliente;

    @BeforeEach
    void preparar() throws Exception {
        r = new ReservasDePrueba(mockMvc, jsonMapper, jdbc, jwtService);
        cliente = r.cliente();
    }

    @Test
    void creaListaYCorrige() throws Exception {
        r.enviar(post(RUTA), cliente.bearer(), datos("Lucía", r.hoy.minusYears(8).toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        org.hamcrest.Matchers.startsWith(RUTA + "/")))
                .andExpect(jsonPath("$.nombre").value("Lucía"))
                .andExpect(jsonPath("$.fechaNacimiento").value(r.hoy.minusYears(8).toString()));
        long pablo = r.crearBeneficiario(cliente.bearer(), "Pablo", r.hoy.minusYears(3));
        r.crearBeneficiario(r.cliente().bearer(), "De otro", r.hoy.minusYears(3));

        r.leer(RUTA, cliente.bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Lucía"))
                .andExpect(jsonPath("$[1].nombre").value("Pablo"));
        r.enviar(put(RUTA + "/" + pablo), cliente.bearer(), datos("Pablo José", r.hoy.minusYears(4).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pablo José"));
        r.leer(RUTA + "/" + pablo, cliente.bearer()).andExpect(jsonPath("$.nombre").value("Pablo José"));
    }

    @Test
    void conLaEdadMinimaYaNoEsMenor() throws Exception {
        r.enviar(post(RUTA), cliente.bearer(), datos("Marcos", r.hoy.minusYears(14).toString()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Ya tiene la edad para crear su propia cuenta."));
        r.enviar(post(RUTA), cliente.bearer(), datos("Marcos", r.hoy.minusYears(14).plusDays(1).toString()))
                .andExpect(status().isCreated());
    }

    @Test
    void datosNoValidosDevuelven400() throws Exception {
        r.enviar(post(RUTA), cliente.bearer(), datos("Lucía", r.hoy.plusDays(1).toString()))
                .andExpect(status().isBadRequest());
        r.enviar(post(RUTA), cliente.bearer(), Map.of("nombre", "Lucía")).andExpect(status().isBadRequest());
    }

    @Test
    void unBeneficiarioAjenoNoExiste() throws Exception {
        long ajeno = r.crearBeneficiario(r.cliente().bearer(), "Pablo", r.hoy.minusYears(3));

        r.leer(RUTA + "/" + ajeno, cliente.bearer()).andExpect(status().isNotFound());
        r.enviar(put(RUTA + "/" + ajeno), cliente.bearer(), datos("Pablo", r.hoy.minusYears(3).toString()))
                .andExpect(status().isNotFound());
        eliminar(ajeno).andExpect(status().isNotFound());
    }

    @Test
    void sinCitasSeBorra() throws Exception {
        long lucia = r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));

        eliminar(lucia).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM beneficiario WHERE id = ?", Integer.class, lucia))
                .isZero();
    }

    @Test
    void conCitasSeAnonimizaYSeCancelanLasFuturas() throws Exception {
        long lucia = r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));
        long futura = idDe(r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", lucia)
                .andExpect(status().isCreated()));
        long pasada = ApiDePrueba.crearCita(jdbc, cliente.id(), r.empleado, r.corte,
                Instant.now().minus(Duration.ofDays(3)));
        jdbc.update("UPDATE cita SET beneficiario_id = ? WHERE id = ?", lucia, pasada);

        eliminar(lucia).andExpect(status().isNoContent());

        assertThat(jdbc.queryForMap("SELECT nombre, apellidos, fecha_nacimiento, eliminado_en FROM beneficiario "
                + "WHERE id = ?", lucia))
                .containsEntry("nombre", "Beneficiario")
                .containsEntry("apellidos", "eliminado")
                .containsEntry("fecha_nacimiento", null)
                .doesNotContainEntry("eliminado_en", null);
        assertThat(jdbc.queryForMap("SELECT estado, motivo_cancelacion FROM cita WHERE id = ?", futura))
                .containsEntry("estado", "CANCELADA")
                .containsEntry("motivo_cancelacion", "Beneficiario eliminado");
        assertThat(jdbc.queryForObject("SELECT estado FROM cita WHERE id = ?", String.class, pasada))
                .isEqualTo("CONFIRMADA");
        r.leer(RUTA, cliente.bearer()).andExpect(jsonPath("$").isEmpty());
        r.leer(RUTA + "/" + lucia, cliente.bearer()).andExpect(status().isNotFound());
    }

    @Test
    void soloLosClientesTienenBeneficiarios() throws Exception {
        r.leer(RUTA, r.admin).andExpect(status().isForbidden());
        r.leer(RUTA, r.bearerEmpleado(r.empleado)).andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions eliminar(long id) throws Exception {
        return mockMvc.perform(delete(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, cliente.bearer()));
    }

    private long idDe(ResultActions resultado) throws Exception {
        return r.api().leer(resultado.andReturn()).get("id").asLong();
    }

    private static Map<String, String> datos(String nombre, String nacimiento) {
        return Map.of("nombre", nombre, "apellidos", "García", "fechaNacimiento", nacimiento);
    }
}
