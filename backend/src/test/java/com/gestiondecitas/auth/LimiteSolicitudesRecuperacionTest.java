package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.gestiondecitas.shared.error.DemasiadosIntentosException;

class LimiteSolicitudesRecuperacionTest {

    private final LimiteSolicitudesRecuperacion limite = new LimiteSolicitudesRecuperacion();

    @Test
    void permiteTresPorCorreoYRechazaLaCuartaDuranteUnaHora() {
        for (int i = 0; i < 3; i++) {
            limite.registrarSolicitud("1.1.1." + i, "ana@ejemplo.com");
        }
        assertThatThrownBy(() -> limite.registrarSolicitud("1.1.1.9", "ana@ejemplo.com"))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(ex -> assertThat(((DemasiadosIntentosException) ex).getSegundosDeEspera())
                        .isBetween(60L, 3601L));
        assertThatCode(() -> limite.registrarSolicitud("1.1.1.9", "otra@ejemplo.com")).doesNotThrowAnyException();
    }

    @Test
    void permiteDiezPorIpAunqueCambieElCorreo() {
        for (int i = 0; i < 10; i++) {
            limite.registrarSolicitud("2.2.2.2", "correo" + i + "@ejemplo.com");
        }
        assertThatThrownBy(() -> limite.registrarSolicitud("2.2.2.2", "otro@ejemplo.com"))
                .isInstanceOf(DemasiadosIntentosException.class);
    }
}
