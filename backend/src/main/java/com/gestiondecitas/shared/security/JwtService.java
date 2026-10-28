package com.gestiondecitas.shared.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite los access tokens (paso 3.4). El {@code sub} es el id del usuario; el
 * token también lleva su rol y si debe cambiar la contraseña (RN-005).
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final PropiedadesSeguridad propiedades;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, PropiedadesSeguridad propiedades, Clock clock) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public String emitir(long usuarioId, String rol, boolean debeCambiarPassword) {
        Instant ahora = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuarioId))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.accessTokenDuracion()))
                .claim(JwtConfig.CLAIM_ROL, rol)
                .claim(JwtConfig.CLAIM_DEBE_CAMBIAR_PASSWORD, debeCambiarPassword)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    public long segundosDeVida() {
        return propiedades.accessTokenDuracion().toSeconds();
    }
}
