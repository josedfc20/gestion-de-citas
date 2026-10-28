package com.gestiondecitas.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;
import com.gestiondecitas.ReservasDePrueba;
import com.gestiondecitas.ReservasDePrueba.Cliente;
import com.gestiondecitas.cita.dto.ReservaRequest;
import com.gestiondecitas.shared.error.HuecoNoDisponibleException;
import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.json.JsonMapper;

/** CU-008 y CU-009: reservar, con RF-007 (volver a comprobar el hueco) y RNF-003 (reservas a la vez). */
@IntegracionTest
class ReservasTest {

    private static final String OCUPADO = "Ese horario ya no está disponible. Elige otra hora.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private TransactionTemplate transaccion;

    private ReservasDePrueba r;
    private Cliente cliente;

    @BeforeEach
    void preparar() throws Exception {
        r = new ReservasDePrueba(mockMvc, jsonMapper, jdbc, jwtService);
        cliente = r.cliente();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM dia_cierre");
    }

    @Test
    void reservaConFinYPrecioDelServidorYOcupaElHueco() throws Exception {
        String inicio = ReservasDePrueba.inicio(r.fecha, "10:00");

        var resultado = r.reservar(cliente.bearer(), r.tinte, r.empleado, r.fecha, "10:00", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.inicio").value(inicio))
                .andExpect(jsonPath("$.fin").value(ReservasDePrueba.inicio(r.fecha, "11:00")))
                .andExpect(jsonPath("$.precio").value(40.00))
                .andExpect(jsonPath("$.servicio.duracionMinutos").value(60))
                .andExpect(jsonPath("$.profesional.id").value(r.empleado))
                .andExpect(jsonPath("$.profesional.nombre").value("Luis G."))
                .andExpect(jsonPath("$.beneficiario").isEmpty())
                .andExpect(jsonPath("$.puedeCancelar").value(true))
                .andExpect(jsonPath("$.cancelableHasta").value(ReservasDePrueba.inicio(r.fecha.minusDays(1), "10:00")))
                .andExpect(jsonPath("$.cliente").doesNotExist())
                .andExpect(jsonPath("$.cambios").doesNotExist())
                .andReturn();
        long id = r.api().leer(resultado).get("id").asLong();
        assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/api/v1/citas/" + id);

        assertThat(jdbc.queryForMap("SELECT estado_anterior, estado_nuevo, realizado_por FROM cambio_estado_cita "
                + "WHERE cita_id = ?", id))
                .containsEntry("estado_anterior", null)
                .containsEntry("estado_nuevo", "CONFIRMADA")
                .containsEntry("realizado_por", cliente.id());
        r.disponibilidad(r.corte, r.empleado, r.fecha)
                .andExpect(jsonPath("$.horas", not(hasItem("10:00"))))
                .andExpect(jsonPath("$.horas", not(hasItem("10:30"))))
                .andExpect(jsonPath("$.horas", hasItem("11:00")));
    }

    @Test
    void reservaParaUnMenorACargo() throws Exception {
        long lucia = r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));

        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", lucia)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.beneficiario.id").value(lucia))
                .andExpect(jsonPath("$.beneficiario.nombre").value("Lucía"));
    }

    @Test
    void unaHoraYaReservadaDevuelve409() throws Exception {
        r.reservarCorte(cliente, "10:00");
        Cliente otro = r.cliente();

        r.reservar(otro.bearer(), r.corte, r.empleado, r.fecha, "10:00", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Hueco no disponible"))
                .andExpect(jsonPath("$.detail").value(OCUPADO));
        // Un tinte de 09:30 a 10:30 también se solapa.
        r.reservar(otro.bearer(), r.tinte, r.empleado, r.fecha, "09:30", null).andExpect(status().isConflict());
    }

    @Test
    void unaHoraQueNoEstaEnLaDisponibilidadDevuelve409() throws Exception {
        // No alineada al intervalo, fuera del horario, el servicio no cabe y día de cierre.
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:15", null).andExpect(status().isConflict());
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "15:00", null).andExpect(status().isConflict());
        r.reservar(cliente.bearer(), r.tinte, r.empleado, r.fecha, "13:30", null).andExpect(status().isConflict());
        r.enviar(post("/api/v1/admin/dias-cierre"), r.admin, Map.of("fecha", r.fecha.toString()))
                .andExpect(status().isCreated());
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", null).andExpect(status().isConflict());
    }

    @Test
    void laHoraConSegundosOEnOtraZonaSeComparaComoInstante() throws Exception {
        // Las 10:00 de Madrid enviadas en UTC son la misma hora.
        String enUtc = OffsetDateTime.parse(ReservasDePrueba.inicio(r.fecha, "10:00"))
                .withOffsetSameInstant(ZoneOffset.UTC).toString();
        r.enviar(post("/api/v1/citas"), cliente.bearer(),
                        Map.of("servicioId", r.corte, "profesionalId", r.empleado, "inicio", enUtc))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value(ReservasDePrueba.inicio(r.fecha, "10:00")));
        String conSegundos = ReservasDePrueba.inicio(r.fecha, "11:00").replace("11:00:00", "11:00:30");
        r.enviar(post("/api/v1/citas"), cliente.bearer(),
                        Map.of("servicioId", r.corte, "profesionalId", r.empleado, "inicio", conSegundos))
                .andExpect(status().isConflict());
    }

    @Test
    void reglasDeFechaServicioYProfesional() throws Exception {
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.hoy.minusDays(1), "10:00", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("La fecha ya ha pasado."));
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.hoy.plusDays(61), "10:00", null)
                .andExpect(status().isUnprocessableContent());
        long barba = r.api().crearServicio(r.admin, ApiDePrueba.nombreUnico("Barba"), 30, "8.00");
        r.reservar(cliente.bearer(), barba, r.empleado, r.fecha, "10:00", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Ese profesional no realiza este servicio."));
        r.reservar(cliente.bearer(), 999999L, r.empleado, r.fecha, "10:00", null).andExpect(status().isNotFound());
        r.reservar(cliente.bearer(), r.corte, 999999L, r.fecha, "10:00", null).andExpect(status().isNotFound());
    }

    @Test
    void elBeneficiarioTieneQueSerSuyoYSeguirSiendoMenor() throws Exception {
        Cliente otro = r.cliente();
        long ajeno = r.crearBeneficiario(otro.bearer(), "Pablo", r.hoy.minusYears(5));
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", ajeno)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El beneficiario no existe."));

        long mayor = r.crearBeneficiario(cliente.bearer(), "Marcos", r.hoy.minusYears(10));
        jdbc.update("UPDATE beneficiario SET fecha_nacimiento = ? WHERE id = ?",
                java.sql.Date.valueOf(r.hoy.minusYears(14)), mayor);
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", mayor)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Ya tiene la edad para crear su propia cuenta."));
    }

    @Test
    void laMismaPersonaNoTieneDosCitasALaVez() throws Exception {
        long otroEmpleado = r.otroEmpleado();
        long lucia = r.crearBeneficiario(cliente.bearer(), "Lucía", r.hoy.minusYears(8));
        r.reservarCorte(cliente, "10:00");

        r.reservar(cliente.bearer(), r.tinte, otroEmpleado, r.fecha, "09:30", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Ya tienes otra cita que coincide con esa hora."));
        // Sí puede reservar a la vez para su hija con otro profesional, pero no dos veces para ella.
        r.reservar(cliente.bearer(), r.corte, otroEmpleado, r.fecha, "10:00", lucia).andExpect(status().isCreated());
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:00", lucia)
                .andExpect(status().isConflict());
        r.reservar(cliente.bearer(), r.corte, r.empleado, r.fecha, "10:30", null).andExpect(status().isCreated());
        long tercero = r.otroEmpleado();
        r.reservar(cliente.bearer(), r.corte, tercero, r.fecha, "10:00", lucia)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Lucía ya tiene otra cita que coincide con esa hora."));
    }

    @Test
    void soloLosClientesReservan() throws Exception {
        r.reservar(null, r.corte, r.empleado, r.fecha, "10:00", null).andExpect(status().isUnauthorized());
        r.reservar(r.admin, r.corte, r.empleado, r.fecha, "10:00", null).andExpect(status().isForbidden());
        r.reservar(r.bearerEmpleado(r.empleado), r.corte, r.empleado, r.fecha, "10:00", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void datosQueFaltanDevuelven400() throws Exception {
        r.enviar(post("/api/v1/citas"), cliente.bearer(), Map.of("servicioId", r.corte, "profesionalId", r.empleado))
                .andExpect(status().isBadRequest());
        r.enviar(post("/api/v1/citas"), cliente.bearer(),
                        Map.of("servicioId", r.corte, "profesionalId", r.empleado, "inicio", "mañana a las 10"))
                .andExpect(status().isBadRequest());
    }

    /** RNF-003: dos clientes pulsan "Reservar" en el mismo hueco a la vez. */
    @Test
    void dosReservasALaVezDelMismoHuecoSoloConfirmanUna() throws Exception {
        List<Cliente> clientes = List.of(cliente, r.cliente(), r.cliente(), r.cliente());
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(clientes.size());
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Cliente c : clientes) {
                resultados.add(hilos.submit(() -> {
                    salida.await();
                    return r.reservar(c.bearer(), r.tinte, r.empleado, r.fecha, "10:00", null)
                            .andReturn().getResponse().getStatus();
                }));
            }
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                estados.add(resultado.get(30, TimeUnit.SECONDS));
            }
            assertThat(estados).containsOnlyOnce(201).containsOnly(201, 409);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE empleado_id = ?", Integer.class,
                r.empleado)).isEqualTo(1);
    }

    /**
     * RNF-003 en el peor caso: la segunda reserva ya ha pasado la comprobación de
     * disponibilidad cuando la primera se guarda. La para la restricción de la base de
     * datos y se responde igual, con el hueco no disponible.
     */
    @Test
    void laRestriccionDeLaBaseDeDatosParaLaReservaQueLlegaTarde() throws Exception {
        Cliente otro = r.cliente();
        var datos = new ReservaRequest(r.corte, r.empleado,
                OffsetDateTime.parse(ReservasDePrueba.inicio(r.fecha, "10:00")), null);
        CountDownLatch primeraGuardada = new CountDownLatch(1);
        CountDownLatch segundaEmpezada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> primera = hilos.submit(() -> transaccion.executeWithoutResult(estado -> {
                reservaService.reservar(cliente.id(), datos);
                primeraGuardada.countDown();
                esperar(segundaEmpezada);
                dormir(1000); // la segunda llega al INSERT y espera a que esta termine
            }));
            primeraGuardada.await(30, TimeUnit.SECONDS);
            Future<?> segunda = hilos.submit((Callable<Object>) () -> {
                segundaEmpezada.countDown();
                return reservaService.reservar(otro.id(), datos);
            });
            primera.get(30, TimeUnit.SECONDS);
            assertThatThrownBy(() -> segunda.get(30, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(HuecoNoDisponibleException.class);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE empleado_id = ?", Integer.class,
                r.empleado)).isEqualTo(1);
    }

    private static void esperar(CountDownLatch senal) {
        try {
            senal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
