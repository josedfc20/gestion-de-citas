package com.gestiondecitas.auth.dto;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.gestiondecitas.auth.PasswordSegura;

/** Datos del formulario de registro (CU-001). */
public record RegistroRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 120) String apellidos,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^\\+?[0-9 ]{6,19}$", message = "Teléfono no válido.") String telefono,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotNull @PasswordSegura String password,
        @NotNull String confirmacionPassword,
        @AssertTrue(message = "Debes aceptar la política de privacidad.") boolean aceptaPrivacidad) {

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isConfirmacionPasswordCoincide() {
        return Objects.equals(password, confirmacionPassword);
    }
}
