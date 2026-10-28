package com.gestiondecitas.empleado.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta de un empleado (CU-017). La contraseña temporal la genera el servidor. */
public record AltaEmpleadoRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 120) String apellidos,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^\\+?[0-9 ]{6,19}$", message = "Teléfono no válido.") String telefono,
        @NotNull Set<@NotNull Long> servicioIds) {
}
