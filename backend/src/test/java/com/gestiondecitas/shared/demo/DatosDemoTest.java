package com.gestiondecitas.shared.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.TestcontainersConfiguration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Perfil {@code demo}: crea la peluquería de demostración al arrancar. Usa su propio
 * contexto (y su propio PostgreSQL) para no mezclar sus datos con las demás pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.demo.password=Clave-de-la-demo-2026")
class DatosDemoTest {

    private static final String PASSWORD = "Clave-de-la-demo-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DatosDemo datosDemo;

    private ApiDePrueba api;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
    }

    @Test
    void creaElCatalogoLosProfesionalesYElHorario() throws Exception {
        mockMvc.perform(get("/api/v1/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        mockMvc.perform(get("/api/v1/profesionales"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Ana M."));
        assertThat(jdbc.queryForObject("SELECT nombre FROM negocio", String.class)).isEqualTo("Peluquería Demo");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tramo_horario WHERE empleado_id IS NULL",
                Integer.class)).isEqualTo(11);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ajuste_horario", Integer.class)).isEqualTo(2);
    }

    @Test
    void lasCuentasEntranConDemoPasswordSinContrasenaTemporal() throws Exception {
        JsonNode luis = api.leer(api.login("luis@demo.invalid", PASSWORD));
        assertThat(luis.get("usuario").get("debeCambiarPassword").asBoolean()).isFalse();
        assertThat(api.login("cliente@demo.invalid", PASSWORD).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unSegundoArranqueNoDuplicaNada() throws Exception {
        datosDemo.run(null);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM servicio", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empleado", Integer.class)).isEqualTo(3);
    }

    @Test
    void hayHorasLibresEnElProximoDiaLaborable() throws Exception {
        long luis = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'luis@demo.invalid'", Long.class);
        long corte = jdbc.queryForObject("SELECT id FROM servicio WHERE nombre = 'Corte de pelo'", Long.class);
        // De martes a viernes trabajan todos; las vacaciones de Luis empiezan dentro de 30 días.
        LocalDate dia = LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(1);
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY
                || dia.getDayOfWeek() == DayOfWeek.MONDAY
                || (dia.getMonthValue() == 12 && dia.getDayOfMonth() == 25)
                || (dia.getMonthValue() == 1 && dia.getDayOfMonth() == 1)) {
            dia = dia.plusDays(1);
        }

        mockMvc.perform(get("/api/v1/disponibilidad").param("servicioId", String.valueOf(corte))
                        .param("profesionalId", String.valueOf(luis)).param("fecha", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas[0]").value("09:00"))
                .andExpect(jsonPath("$.horas.length()").value(18));
    }
}
