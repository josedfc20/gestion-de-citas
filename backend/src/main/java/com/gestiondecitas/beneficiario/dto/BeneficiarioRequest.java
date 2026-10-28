package com.gestiondecitas.beneficiario.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Solo los datos imprescindibles del menor (CU-009, RNF-007). */
public record BeneficiarioRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 120) String apellidos,
        @NotNull @PastOrPresent LocalDate fechaNacimiento) {
}
