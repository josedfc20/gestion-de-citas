package com.gestiondecitas.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el usuario puede cambiar de su perfil (CU-004). */
public record ActualizarPerfilRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 120) String apellidos,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^\\+?[0-9 ]{6,19}$", message = "Teléfono no válido.") String telefono) {
}
