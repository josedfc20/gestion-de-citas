package com.gestiondecitas.shared.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie del refresh token (paso 3.4): {@code HttpOnly} para que JavaScript no
 * pueda leerla, {@code SameSite=Strict} contra CSRF y limitada a las rutas de
 * {@code /api/v1/auth}, que son las únicas que la necesitan (refresh y logout).
 */
@Component
public class CookieRefreshToken {

    public static final String NOMBRE = "refresh_token";
    static final String RUTA = "/api/v1/auth";

    private final PropiedadesSeguridad propiedades;

    public CookieRefreshToken(PropiedadesSeguridad propiedades) {
        this.propiedades = propiedades;
    }

    public ResponseCookie crear(String refreshToken) {
        return construir(refreshToken, propiedades.refreshTokenDuracion());
    }

    /** Una cookie vacía con duración 0 hace que el navegador borre la anterior. */
    public ResponseCookie borrar() {
        return construir("", Duration.ZERO);
    }

    private ResponseCookie construir(String valor, Duration duracion) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(propiedades.cookieSecure())
                .sameSite("Strict")
                .path(RUTA)
                .maxAge(duracion)
                .build();
    }
}
