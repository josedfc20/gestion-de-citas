package com.gestiondecitas.auth;

import com.gestiondecitas.auth.dto.SesionResponse;

/**
 * Resultado interno de iniciar o renovar una sesión: el cuerpo de la respuesta
 * y el refresh token en claro, que el controlador pone en la cookie.
 */
public record SesionEmitida(SesionResponse respuesta, String refreshToken) {
}
