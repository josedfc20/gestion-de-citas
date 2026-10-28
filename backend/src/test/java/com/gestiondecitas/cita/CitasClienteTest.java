package com.gestiondecitas.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.ReservasDePrueba;
import com.gestiondecitas.ReservasDePrueba.Cliente;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-010 a CU-012: mis citas, detalle y cancelar (RN-004). */
@IntegracionTest
class CitasClienteTest {

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
    void misCitasMuestraLasProximasEnOrdenYElHistorialAparte() throws Exception {
        long tarde = r.reservarCorte(cliente, "17:00");
        long manana = r.reservarCorte(cliente, "10:00");
        long cancelada = r.reservarCorte(cliente, "12:00");
        cancelar(cliente.bearer(), cancelada).andExpect(status().isOk());
        long pasada = ApiDePrueba.crearCita(jdbc, cliente.id(), r.empleado, r.corte,
                Instant.now().minus(Duration.ofDays(2)));
        r.reservarCorte(r.cliente(), "11:00"); // de otro cliente

        r.leer("/api/v1/me/citas", cliente.bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[0].id").value(manana))
                .andExpect(jsonPath("$.contenido[1].id").value(cancelada))
                .andExpect(jsonPath("$.contenido[1].estado").value("CANCELADA"))
                .andExpect(jsonPath("$.contenido[1].puedeCancelar").value(false))
                .andExpect(jsonPath("$.contenido[2].id").value(tarde));
        r.leer("/api/v1/me/citas?pasadas=true", cliente.bearer())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(pasada))
                .andExpect(jsonPath("$.contenido[0].puedeCancelar").value(false));
        r.leer("/api/v1/me/citas?page=1&size=2", cliente.bearer())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido.length()").value(1));
        r.leer("/api/v1/me/citas?size=500", cliente.bearer()).andExpect(status().isBadRequest());
        r.leer("/api/v1/me/citas", r.bearerEmpleado(r.empleado)).andExpect(status().isForbidden());
    }

    @Test
    void cadaUnoVeElDetalleDeLoSuyo() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");
        String ruta = "/api/v1/citas/" + cita;

        r.leer(ruta, cliente.bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cita))
                .andExpect(jsonPath("$.cliente").doesNotExist());
        r.leer(ruta, r.cliente().bearer())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La cita no existe."));
        // El empleado asignado ve el nombre y el teléfono del cliente, pero no su correo.
        r.leer(ruta, r.bearerEmpleado(r.empleado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nombre").value("Ana"))
                .andExpect(jsonPath("$.cliente.telefono").value("600123123"))
                .andExpect(jsonPath("$.cliente.email").doesNotExist())
                .andExpect(jsonPath("$.cambios").doesNotExist());
        r.leer(ruta, r.bearerEmpleado(r.otroEmpleado())).andExpect(status().isNotFound());
        // El administrador ve también el correo y el historial.
        r.leer(ruta, r.admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.email").value(cliente.email()))
                .andExpect(jsonPath("$.cambios.length()").value(1))
                .andExpect(jsonPath("$.cambios[0].estadoNuevo").value("CONFIRMADA"))
                .andExpect(jsonPath("$.cambios[0].realizadoPor").value(cliente.id()));
        r.leer("/api/v1/citas/999999", r.admin).andExpect(status().isNotFound());
    }

    @Test
    void cancelarLiberaElHueco() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");

        r.enviar(post("/api/v1/citas/" + cita + "/cancelacion"), cliente.bearer(), Map.of("motivo", "No puedo ir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelacion").value("No puedo ir"))
                .andExpect(jsonPath("$.puedeCancelar").value(false));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cambio_estado_cita WHERE cita_id = ?", Integer.class,
                cita)).isEqualTo(2);
        r.disponibilidad(r.corte, r.empleado, r.fecha).andExpect(jsonPath("$.horas", hasItem("10:00")));
        cancelar(cliente.bearer(), cita)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Esta cita ya no se puede cambiar: no está confirmada."));
    }

    @Test
    void conMenosDe24HorasNoSeCancelaOnline() throws Exception {
        long cita = ApiDePrueba.crearCita(jdbc, cliente.id(), r.empleado, r.corte,
                Instant.now().plus(Duration.ofHours(5)));

        r.leer("/api/v1/citas/" + cita, cliente.bearer()).andExpect(jsonPath("$.puedeCancelar").value(false));
        cancelar(cliente.bearer(), cita)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail")
                        .value("Ya no puedes cancelar online: faltan menos de 24 horas. Contacta con el negocio."));
    }

    @Test
    void soloElClienteCancelaLaSuya() throws Exception {
        long cita = r.reservarCorte(cliente, "10:00");

        cancelar(r.cliente().bearer(), cita).andExpect(status().isNotFound());
        cancelar(r.bearerEmpleado(r.empleado), cita).andExpect(status().isForbidden());
        cancelar(r.admin, cita).andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions cancelar(String bearer, long cita) throws Exception {
        return mockMvc.perform(post("/api/v1/citas/" + cita + "/cancelacion")
                .header(org.springframework.http.HttpHeaders.AUTHORIZATION, bearer));
    }
}
