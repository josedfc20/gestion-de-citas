package com.gestiondecitas.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.Empleado;

/** Transiciones del diagrama {@code Estados-Cita} y plazo de cancelación (RN-004, RN-008). */
class CitaTest {

    private static final Instant INICIO = Instant.parse("2030-01-07T10:00:00Z");
    private static final Duration PLAZO = Duration.ofHours(24);
    private static final Long CLIENTE = 1L;
    private static final Long EMPLEADO = 2L;

    private Cita cita;

    @BeforeEach
    void reservar() {
        Cliente cliente = new Cliente("Ana", "García", "ana@pruebas.local", null, "hash", LocalDate.of(1990, 1, 1),
                INICIO.minus(Duration.ofDays(10)));
        Empleado empleado = new Empleado("Luis", "Pérez", "luis@pruebas.local", null, "hash", INICIO);
        Servicio servicio = new Servicio("Tinte", null, 90, new BigDecimal("40.00"));
        cita = Cita.reservar(cliente, empleado, servicio, null, INICIO, CLIENTE, INICIO.minus(Duration.ofDays(3)));
    }

    @Test
    void alReservarCalculaElFinCopiaElPrecioYGuardaElPrimerCambio() {
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(cita.getFin()).isEqualTo(INICIO.plus(Duration.ofMinutes(90)));
        assertThat(cita.getPrecio()).isEqualByComparingTo("40.00");
        assertThat(cita.getCambios()).singleElement().satisfies(c -> {
            assertThat(c.getEstadoAnterior()).isNull();
            assertThat(c.getEstadoNuevo()).isEqualTo(EstadoCita.CONFIRMADA);
            assertThat(c.getRealizadoPor()).isEqualTo(CLIENTE);
        });
    }

    @Test
    void elClienteCancelaJustoEnElLimiteDe24Horas() {
        Instant limite = INICIO.minus(PLAZO);
        assertThat(cita.puedeCancelarCliente(PLAZO, limite)).isTrue();

        cita.cancelarPorCliente("  No puedo ir  ", PLAZO, CLIENTE, limite);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.getMotivoCancelacion()).isEqualTo("No puedo ir");
        assertThat(cita.getCambios()).hasSize(2).last()
                .satisfies(c -> assertThat(c.getEstadoAnterior()).isEqualTo(EstadoCita.CONFIRMADA));
    }

    @Test
    void unMotivoEnBlancoSeGuardaVacio() {
        cita.cancelarPorCliente("   ", PLAZO, CLIENTE, INICIO.minus(PLAZO));

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.getMotivoCancelacion()).isNull();
    }

    @Test
    void elClienteNoCancelaConMenosDe24Horas() {
        Instant tarde = INICIO.minus(PLAZO).plusSeconds(60);
        assertThat(cita.puedeCancelarCliente(PLAZO, tarde)).isFalse();

        assertThatThrownBy(() -> cita.cancelarPorCliente(null, PLAZO, CLIENTE, tarde))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Contacta con el negocio");
        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
    }

    @Test
    void elEmpleadoCancelaEnCualquierMomentoPeroConMotivo() {
        Instant ahora = INICIO.minusSeconds(600);
        assertThatThrownBy(() -> cita.cambiarEstado(EstadoCita.CANCELADA, " ", EMPLEADO, ahora))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Indica el motivo de la cancelación.");

        cita.cambiarEstado(EstadoCita.CANCELADA, "Enfermedad del profesional", EMPLEADO, ahora);

        assertThat(cita.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        assertThat(cita.getCambios()).last().satisfies(c -> assertThat(c.getRealizadoPor()).isEqualTo(EMPLEADO));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "NO_PRESENTADO"})
    void completadaONoPresentadoSoloDesdeLaHoraDeInicio(EstadoCita nuevo) {
        assertThatThrownBy(() -> cita.cambiarEstado(nuevo, null, EMPLEADO, INICIO.minusSeconds(1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("antes de la hora de inicio");

        cita.cambiarEstado(nuevo, null, EMPLEADO, INICIO);

        assertThat(cita.getEstado()).isEqualTo(nuevo);
        assertThat(cita.getMotivoCancelacion()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "NO_PRESENTADO", "CANCELADA"})
    void losEstadosFinalesNoCambian(EstadoCita final_) {
        cita.cambiarEstado(final_, "Motivo", EMPLEADO, INICIO);

        for (EstadoCita nuevo : EstadoCita.values()) {
            assertThatThrownBy(() -> cita.cambiarEstado(nuevo, "Motivo", EMPLEADO, INICIO.plusSeconds(60)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage(Cita.NO_CONFIRMADA);
        }
        assertThatThrownBy(() -> cita.cancelarPorCliente(null, PLAZO, CLIENTE, INICIO.minus(Duration.ofDays(2))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(cita.puedeCancelarCliente(PLAZO, INICIO.minus(Duration.ofDays(2)))).isFalse();
    }

    @Test
    void noSePuedeVolverAConfirmar() {
        assertThatThrownBy(() -> cita.cambiarEstado(EstadoCita.CONFIRMADA, null, EMPLEADO, INICIO))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
