package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PoliticaPasswordTest {

    @Test
    void aceptaUnaContrasenaLargaYPocoComun() {
        assertThat(PoliticaPassword.incumplimiento("Mi perro se llama Toby")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "corta", "123456789"})
    void rechazaLasDeMenosDeDiezCaracteres(String password) {
        assertThat(PoliticaPassword.incumplimiento(password)).contains("Debe tener entre 10 y 64 caracteres.");
    }

    @Test
    void rechazaLasDeMasDeSesentaYCuatroCaracteres() {
        assertThat(PoliticaPassword.incumplimiento("a".repeat(65))).isPresent();
        assertThat(PoliticaPassword.incumplimiento("ñ".repeat(64))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "QWERTYUIOP", "Contraseña123", "cambia-esta-contrasena"})
    void rechazaLasMuyComunesSinDistinguirMayusculas(String password) {
        assertThat(PoliticaPassword.incumplimiento(password)).contains("Es una contraseña demasiado común; elige otra.");
    }

    @Test
    void noHayReglasDeSimbolos() {
        assertThat(PoliticaPassword.incumplimiento("solominusculaslargas")).isEmpty();
    }
}
