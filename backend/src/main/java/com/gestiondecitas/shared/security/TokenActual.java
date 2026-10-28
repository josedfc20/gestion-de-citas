package com.gestiondecitas.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Lecturas del access token de la petición actual. */
final class TokenActual {

    private TokenActual() {
    }

    static boolean esJwt(Authentication autenticacion) {
        return autenticacion instanceof JwtAuthenticationToken;
    }

    static boolean debeCambiarPassword(Authentication autenticacion) {
        return autenticacion instanceof JwtAuthenticationToken jwt
                && Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean(JwtConfig.CLAIM_DEBE_CAMBIAR_PASSWORD));
    }
}
