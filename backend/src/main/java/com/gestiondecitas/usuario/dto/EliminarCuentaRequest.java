package com.gestiondecitas.usuario.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmación para eliminar la cuenta: la contraseña actual (CU-004, alt. 3). */
public record EliminarCuentaRequest(@NotBlank String password) {
}
