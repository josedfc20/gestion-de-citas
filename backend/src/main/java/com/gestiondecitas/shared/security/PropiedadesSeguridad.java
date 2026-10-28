package com.gestiondecitas.shared.security;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Valores técnicos de la seguridad ({@code app.security.*} en application.yml).
 * Si {@code JWT_SECRET} falta o es corto, la aplicación no arranca y lo dice.
 *
 * @param jwtSecret             clave para firmar los JWT con HS256 (mínimo 32 caracteres)
 * @param accessTokenDuracion   vida del access token (15 min, paso 3.4)
 * @param refreshTokenDuracion  vida del refresh token (7 días, paso 3.4)
 * @param cookieSecure          atributo {@code Secure} de la cookie; los navegadores lo aceptan en localhost
 * @param intentosLoginPorMinuto límite de intentos de login por IP y por correo
 */
@Validated
@ConfigurationProperties("app.security")
public record PropiedadesSeguridad(
        @NotBlank(message = "falta JWT_SECRET en el .env o en las variables de entorno")
        @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres")
        String jwtSecret,
        @NotNull Duration accessTokenDuracion,
        @NotNull Duration refreshTokenDuracion,
        boolean cookieSecure,
        @Min(1) int intentosLoginPorMinuto) {
}
