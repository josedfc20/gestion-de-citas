package com.gestiondecitas.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Emisión y validación del access token con un reloj fijo. */
class JwtServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-28T10:00:00Z");

    private final PropiedadesSeguridad propiedades = new PropiedadesSeguridad(
            "clave-de-prueba-de-al-menos-32-caracteres", Duration.ofMinutes(15), Duration.ofDays(7), true, 5);
    private final JwtConfig config = new JwtConfig();
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final JwtService service = new JwtService(
            config.jwtEncoder(config.claveJwt(propiedades)), propiedades, reloj);

    @Test
    void elTokenLlevaElUsuarioElRolYDuraQuinceMinutos() {
        Jwt jwt = decoder(reloj).decode(service.emitir(7, "EMPLEADO", true));

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("EMPLEADO");
        assertThat(jwt.getClaimAsBoolean("dcp")).isTrue();
        assertThat(jwt.getIssuedAt()).isEqualTo(AHORA);
        assertThat(jwt.getExpiresAt()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(service.segundosDeVida()).isEqualTo(900);
    }

    @Test
    void pasadoElPlazoSeRechaza() {
        String token = service.emitir(7, "CLIENTE", false);
        // 15 minutos de vida + 60 segundos de margen por diferencias de reloj.
        Clock masTarde = Clock.offset(reloj, Duration.ofMinutes(17));

        assertThatThrownBy(() -> decoder(masTarde).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        var otraClave = new PropiedadesSeguridad("otra-clave-distinta-de-al-menos-32-caracteres",
                Duration.ofMinutes(15), Duration.ofDays(7), true, 5);
        String ajeno = new JwtService(config.jwtEncoder(config.claveJwt(otraClave)), otraClave, reloj)
                .emitir(1, "ADMIN", false);

        assertThatThrownBy(() -> decoder(reloj).decode(ajeno)).isInstanceOf(JwtException.class);
    }

    private JwtDecoder decoder(Clock clock) {
        return config.jwtDecoder(config.claveJwt(propiedades), clock);
    }
}
