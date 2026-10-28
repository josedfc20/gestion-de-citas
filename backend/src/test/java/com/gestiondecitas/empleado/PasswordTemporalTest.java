package com.gestiondecitas.empleado;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.gestiondecitas.auth.PoliticaPassword;

/** RN-005: contraseña temporal y nombre público del profesional. */
class PasswordTemporalTest {

    @Test
    void tieneDoceCaracteresSinAmbiguosYCumpleLaPolitica() {
        Set<String> generadas = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String password = PasswordTemporal.generar();
            assertThat(password).hasSize(12).doesNotContainAnyWhitespaces();
            assertThat(password.chars()).allMatch(c -> PasswordTemporal.ALFABETO.indexOf(c) >= 0);
            assertThat(password).doesNotContain("0", "O", "1", "l", "I");
            assertThat(PoliticaPassword.incumplimiento(password)).isEmpty();
            generadas.add(password);
        }
        assertThat(generadas).hasSize(200);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Luis|García López|Luis G.",
        "Ana| de la Fuente|Ana D.",
        "Óscar|álvarez|Óscar Á.",
        "Marta|''|Marta"})
    void nombrePublicoConLaInicialDelApellido(String nombre, String apellidos, String esperado) {
        assertThat(EmpleadoService.nombrePublico(nombre, apellidos)).isEqualTo(esperado);
    }
}
