package com.gestiondecitas.auth.dto;

/** Respuesta de login, refresh y cambio de contraseña. El refresh token va en la cookie. */
public record SesionResponse(String accessToken, String tokenType, long expiraEnSegundos, UsuarioResumen usuario) {

    public SesionResponse(String accessToken, long expiraEnSegundos, UsuarioResumen usuario) {
        this(accessToken, "Bearer", expiraEnSegundos, usuario);
    }
}
