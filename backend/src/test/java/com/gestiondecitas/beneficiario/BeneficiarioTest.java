package com.gestiondecitas.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** RN-007: es menor mientras no tenga la edad mínima para crear una cuenta. */
class BeneficiarioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 28);

    @Test
    void elDiaDeSuCumpleanosDejaDeSerMenor() {
        Beneficiario cumpleHoy = new Beneficiario(1L, "Lucía", "Pérez", HOY.minusYears(14));
        Beneficiario cumpleManana = new Beneficiario(1L, "Lucía", "Pérez", HOY.minusYears(14).plusDays(1));

        assertThat(cumpleHoy.esMenor(HOY, 14)).isFalse();
        assertThat(cumpleManana.esMenor(HOY, 14)).isTrue();
        assertThat(cumpleHoy.esMenor(HOY, 16)).isTrue();
    }

    @Test
    void anonimizarBorraSusDatos() {
        Beneficiario beneficiario = new Beneficiario(1L, "Lucía", "Pérez", HOY.minusYears(8));

        beneficiario.anonimizar(Instant.parse("2026-10-28T10:00:00Z"));

        assertThat(beneficiario.isEliminado()).isTrue();
        assertThat(beneficiario.getNombre()).isEqualTo("Beneficiario");
        assertThat(beneficiario.getApellidos()).isEqualTo("eliminado");
        assertThat(beneficiario.getFechaNacimiento()).isNull();
        assertThat(beneficiario.esMenor(HOY, 14)).isFalse();
    }
}
