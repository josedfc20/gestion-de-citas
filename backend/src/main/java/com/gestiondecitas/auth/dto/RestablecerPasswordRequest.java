package com.gestiondecitas.auth.dto;

import java.util.Objects;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.gestiondecitas.auth.PasswordSegura;

/** Token del enlace y nueva contraseña escrita dos veces (CU-020, paso 4). */
public record RestablecerPasswordRequest(
        @NotBlank @Size(max = 100) String token,
        @NotNull @PasswordSegura String password,
        @NotNull String confirmacionPassword) {

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isConfirmacionPasswordCoincide() {
        return Objects.equals(password, confirmacionPassword);
    }
}
