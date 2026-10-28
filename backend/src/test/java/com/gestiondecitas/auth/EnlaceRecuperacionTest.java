package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.gestiondecitas.shared.correo.Correo;

/** Caducidad y un solo uso del enlace, y texto del correo (CU-020). */
class EnlaceRecuperacionTest {

    private static final Instant CREADO = Instant.parse("2026-10-28T10:00:00Z");

    private final TokenRecuperacion token =
            new TokenRecuperacion(null, "hash", CREADO.plus(RecuperacionPasswordService.DURACION_ENLACE));

    @Test
    void valeDuranteTreintaMinutos() {
        assertThat(RecuperacionPasswordService.DURACION_ENLACE).isEqualTo(Duration.ofMinutes(30));
        assertThat(token.esValido(CREADO.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(token.esValido(CREADO.plus(Duration.ofMinutes(30)))).isFalse();
        assertThat(token.esValido(CREADO.plus(Duration.ofMinutes(31)))).isFalse();
    }

    @Test
    void soloSePuedeUsarUnaVez() {
        token.usar();
        assertThat(token.esValido(CREADO)).isFalse();
    }

    @Test
    void elEnlaceLlevaElTokenDetrasDeLaAlmohadilla() {
        assertThat(CorreoRecuperacion.enlace("http://localhost:5173/", "abc"))
                .isEqualTo("http://localhost:5173/restablecer-password#token=abc");
        assertThat(CorreoRecuperacion.enlace("https://citas.ejemplo.com", "abc"))
                .isEqualTo("https://citas.ejemplo.com/restablecer-password#token=abc");
    }

    @Test
    void elCorreoTieneElEnlaceYAvisaDeLaCaducidad() {
        Correo correo = new CorreoRecuperacion("ana@ejemplo.com", "Ana", "Peluquería Moderna",
                "http://web/restablecer-password#token=abc").correo();

        assertThat(correo.para()).isEqualTo("ana@ejemplo.com");
        assertThat(correo.asunto()).isEqualTo("Restablecer tu contraseña · Peluquería Moderna");
        assertThat(correo.texto())
                .startsWith("Hola, Ana:")
                .contains("http://web/restablecer-password#token=abc")
                .contains("caduca en 30 minutos")
                .contains("Si no lo has pedido tú, ignora este correo");
    }

    @Test
    void elTokenNoApareceEnElLog() {
        assertThat(new CorreoRecuperacion("ana@ejemplo.com", "Ana", "N", "http://web#token=secreto").toString())
                .doesNotContain("secreto").doesNotContain("ana@ejemplo.com");
    }
}
