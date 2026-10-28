package com.gestiondecitas.auth.dto;

import java.util.Objects;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.gestiondecitas.auth.PasswordSegura;

/** Cambio de contraseña (CU-004) o de la contraseña temporal del empleado (RN-005). */
public record CambioPasswordRequest(
        @NotBlank String passwordActual,
        @NotNull @PasswordSegura String passwordNueva,
        @NotNull String confirmacionPassword) {

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isConfirmacionPasswordCoincide() {
        return Objects.equals(passwordNueva, confirmacionPassword);
    }
}
