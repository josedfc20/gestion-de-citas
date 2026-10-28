package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.gestiondecitas.shared.error.DemasiadosIntentosException;
import com.gestiondecitas.shared.security.PropiedadesSeguridad;

class LimiteIntentosLoginTest {

    private final LimiteIntentosLogin limite = new LimiteIntentosLogin(new PropiedadesSeguridad(
            "x".repeat(32), Duration.ofMinutes(15), Duration.ofDays(7), true, 5));

    @Test
    void permiteCincoIntentosYRechazaElSexto() {
        for (int i = 0; i < 5; i++) {
            limite.registrarIntento("1.1.1.1", "ana@ejemplo.com");
        }
        assertThatThrownBy(() -> limite.registrarIntento("1.1.1.1", "ana@ejemplo.com"))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions
                        .assertThat(((DemasiadosIntentosException) ex).getSegundosDeEspera()).isBetween(1L, 61L));
    }

    @Test
    void cuentaPorIpAunqueCambieElCorreo() {
        for (int i = 0; i < 5; i++) {
            limite.registrarIntento("2.2.2.2", "correo" + i + "@ejemplo.com");
        }
        assertThatThrownBy(() -> limite.registrarIntento("2.2.2.2", "otro@ejemplo.com"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void cuentaPorCorreoAunqueCambieLaIp() {
        for (int i = 0; i < 5; i++) {
            limite.registrarIntento("3.3.3." + i, "luis@ejemplo.com");
        }
        assertThatThrownBy(() -> limite.registrarIntento("3.3.3.99", "luis@ejemplo.com"))
                .isInstanceOf(DemasiadosIntentosException.class);
        assertThatCode(() -> limite.registrarIntento("3.3.3.99", "otra@ejemplo.com")).doesNotThrowAnyException();
    }
}
