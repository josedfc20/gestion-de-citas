package com.gestiondecitas.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales de CU-002. Sin {@code @Email}: un formato raro solo es "credenciales no válidas". */
public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 200) String password) {
}
