package com.gestiondecitas.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.gestiondecitas.auth.dto.SesionResponse;
import com.gestiondecitas.auth.dto.UsuarioResumen;
import com.gestiondecitas.shared.security.JwtService;
import com.gestiondecitas.shared.security.PropiedadesSeguridad;
import com.gestiondecitas.usuario.Usuario;

/**
 * Crea la pareja access token + refresh token de una sesión. El refresh token
 * son 32 bytes aleatorios; en la base de datos solo se guarda su SHA-256.
 */
@Component
class TokensDeSesion {

    private final SecureRandom aleatorio = new SecureRandom();
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokens;
    private final PropiedadesSeguridad propiedades;
    private final Clock clock;

    TokensDeSesion(JwtService jwtService, RefreshTokenRepository refreshTokens, PropiedadesSeguridad propiedades,
            Clock clock) {
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    SesionEmitida emitir(Usuario usuario) {
        String accessToken = jwtService.emitir(usuario.getId(), usuario.getRol().name(),
                usuario.debeCambiarPassword());

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ahora = clock.instant();
        refreshTokens.save(new RefreshToken(usuario, hash(refreshToken), ahora,
                ahora.plus(propiedades.refreshTokenDuracion())));

        SesionResponse respuesta = new SesionResponse(accessToken, jwtService.segundosDeVida(),
                UsuarioResumen.de(usuario));
        return new SesionEmitida(respuesta, refreshToken);
    }

    static String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
