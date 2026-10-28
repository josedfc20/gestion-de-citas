package com.gestiondecitas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Correo de la cuenta que ha olvidado la contraseña (CU-020, paso 1). */
public record RecuperarPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {
}
