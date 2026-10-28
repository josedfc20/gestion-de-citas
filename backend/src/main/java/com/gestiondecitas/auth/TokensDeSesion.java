package com.gestiondecitas.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.gestiondecitas.auth.dto.SesionResponse;
import com.gestiondecitas.auth.dto.UsuarioResumen;
import com.gestiondecitas.shared.security.JwtService;
import com.gestiondecitas.shared.security.PropiedadesSeguridad;
import com.gestiondecitas.usuario.Usuario;

/**
 * Crea la pareja access token + refresh token de una sesión. El refresh token
 * es un {@link TokenAleatorio}; en la base de datos solo se guarda su SHA-256.
 */
@Component
class TokensDeSesion {

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

        String refreshToken = TokenAleatorio.generar();
        Instant ahora = clock.instant();
        refreshTokens.save(new RefreshToken(usuario, hash(refreshToken), ahora,
                ahora.plus(propiedades.refreshTokenDuracion())));

        SesionResponse respuesta = new SesionResponse(accessToken, jwtService.segundosDeVida(),
                UsuarioResumen.de(usuario));
        return new SesionEmitida(respuesta, refreshToken);
    }

    static String hash(String refreshToken) {
        return TokenAleatorio.sha256(refreshToken);
    }
}
