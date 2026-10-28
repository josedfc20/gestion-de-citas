package com.gestiondecitas.cita;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.ReservasDePrueba;
import com.gestiondecitas.ReservasDePrueba.Cliente;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-019: el administrador consulta todas las reservas, reserva por un cliente y busca clientes. */
@IntegracionTest
class AdminCitasTest {

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
    private Long adminId;

    @BeforeEach
    void preparar() throws Exception {
        r = new ReservasDePrueba(mockMvc, jsonMapper, jdbc, jwtService);
        cliente = r.cliente();
        adminId = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'admin@pruebas.local'", Long.class);
    }

    @Test
    void reservaEnNombreDeUnClienteConLasMismasReglas() throws Exception {
        long lucia = r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));

        long cita = r.api().leer(reservar(cliente.id(), "10:00", lucia)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.beneficiario.nombre").value("Lucía"))
                        .andExpect(jsonPath("$.cliente.email").value(cliente.email()))
                        .andExpect(jsonPath("$.cambios[0].realizadoPor").value(adminId))
                        .andReturn()).get("id").asLong();

        // El cliente la ve entre sus citas.
        r.leer("/api/v1/me/citas", cliente.bearer()).andExpect(jsonPath("$.contenido[0].id").value(cita));
        reservar(r.cliente().id(), "10:00", null)
                .andExpect(status().isConflict());
        reservar(cliente.id(), "11:00", null)
                .andExpect(status().isCreated());
        reservar(cliente.id(), "12:00", lucia).andExpect(status().isCreated());
        reservar(999999L, "12:30", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El cliente no existe."));
        // El id de un empleado no es un cliente.
        reservar(r.empleado, "12:30", null).andExpect(status().isNotFound());
        r.enviar(post("/api/v1/admin/citas"), cliente.bearer(), Map.of()).andExpect(status().isForbidden());
    }

    @Test
    void elAdminSabeQueElClienteYaTieneCita() throws Exception {
        r.reservarCorte(cliente, "10:00");
        long otro = r.otroEmpleado();
        Map<String, Object> cuerpo = new HashMap<>(Map.of("clienteId", cliente.id(), "servicioId", r.corte,
                "profesionalId", otro, "inicio", ReservasDePrueba.inicio(r.fecha, "10:00")));

        r.enviar(post("/api/v1/admin/citas"), r.admin, cuerpo)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("El cliente ya tiene otra cita que coincide con esa hora."));
    }

    @Test
    void listaTodasLasReservasConFiltros() throws Exception {
        long otroEmpleado = r.otroEmpleado();
        Cliente otro = r.cliente();
        long a = r.reservarCorte(cliente, "10:00");
        long b = r.api().leer(r.reservar(otro.bearer(), r.corte, otroEmpleado, r.fecha, "09:00", null)
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        r.leer("/api/v1/admin/citas?profesionalId=" + r.empleado, r.admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(a))
                .andExpect(jsonPath("$.contenido[0].cliente.email").value(cliente.email()))
                .andExpect(jsonPath("$.contenido[0].cambios").doesNotExist());
        r.leer("/api/v1/admin/citas?clienteId=" + otro.id(), r.admin)
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(b));
        r.leer("/api/v1/admin/citas?clienteId=" + otro.id() + "&estado=CANCELADA", r.admin)
                .andExpect(jsonPath("$.totalElementos").value(0));
        r.leer("/api/v1/admin/citas?clienteId=" + otro.id() + "&desde=" + r.fecha.plusDays(1), r.admin)
                .andExpect(jsonPath("$.totalElementos").value(0));
        r.leer("/api/v1/admin/citas?desde=" + r.fecha + "&hasta=" + r.fecha.minusDays(1), r.admin)
                .andExpect(status().isBadRequest());
        r.leer("/api/v1/admin/citas", r.bearerEmpleado(r.empleado)).andExpect(status().isForbidden());
    }

    @Test
    void buscaClientesPorNombreCorreoOTelefonoConSusMenores() throws Exception {
        jdbc.update("UPDATE usuario SET nombre = 'Zacarías', apellidos = 'Buscable Único', telefono = '699111222' "
                + "WHERE id = ?", cliente.id());
        r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));

        buscar("buscable ún")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(cliente.id()))
                .andExpect(jsonPath("$[0].email").value(cliente.email()))
                .andExpect(jsonPath("$[0].beneficiarios[0].nombre").value("Lucía"));
        buscar(cliente.email().substring(0, 12).toUpperCase()).andExpect(jsonPath("$[0].id").value(cliente.id()));
        buscar("699111").andExpect(jsonPath("$[0].id").value(cliente.id()));
        buscar("%%%").andExpect(jsonPath("$").isEmpty());
        buscar("ab").andExpect(status().isBadRequest());
        // Los empleados no salen: solo clientes activos.
        buscar("Luis García").andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/admin/clientes").param("buscar", "buscable")
                        .header("Authorization", cliente.bearer()))
                .andExpect(status().isForbidden());
    }

    private ResultActions buscar(String texto) throws Exception {
        return mockMvc.perform(get("/api/v1/admin/clientes").param("buscar", texto)
                .header("Authorization", r.admin));
    }

    private ResultActions reservar(long clienteId, String hora, Long beneficiarioId) throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("clienteId", clienteId);
        cuerpo.put("servicioId", r.corte);
        cuerpo.put("profesionalId", r.empleado);
        cuerpo.put("inicio", ReservasDePrueba.inicio(r.fecha, hora));
        cuerpo.put("beneficiarioId", beneficiarioId);
        return r.enviar(post("/api/v1/admin/citas"), r.admin, cuerpo);
    }
}
