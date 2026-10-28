package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** RN-006 con fechas fijas (en la aplicación, "hoy" sale del Clock en la zona del negocio). */
class EdadMinimaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 28);

    @Test
    void cumpleCatorceJustoHoy() {
        assertThat(AuthService.tieneEdadMinima(LocalDate.of(2012, 10, 28), HOY, 14)).isTrue();
    }

    @Test
    void leFaltaUnDia() {
        assertThat(AuthService.tieneEdadMinima(LocalDate.of(2012, 10, 29), HOY, 14)).isFalse();
    }

    @Test
    void nacidoUnVeintinueveDeFebrero() {
        // En años no bisiestos, Period cuenta el cumpleaños el 1 de marzo.
        LocalDate nacimiento = LocalDate.of(2012, 2, 29);
        assertThat(AuthService.tieneEdadMinima(nacimiento, LocalDate.of(2026, 2, 28), 14)).isFalse();
        assertThat(AuthService.tieneEdadMinima(nacimiento, LocalDate.of(2026, 3, 1), 14)).isTrue();
    }

    @Test
    void laEdadMinimaEsConfigurable() {
        assertThat(AuthService.tieneEdadMinima(LocalDate.of(2010, 1, 1), HOY, 18)).isFalse();
    }
}
