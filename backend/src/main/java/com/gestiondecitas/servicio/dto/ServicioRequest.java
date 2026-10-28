package com.gestiondecitas.servicio.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta o edición de un servicio (CU-016). RN-011 se comprueba en el servicio (422). */
public record ServicioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 1000) String descripcion,
        @NotNull @Min(1) @Max(480) Integer duracionMinutos,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999.99")
        @Digits(integer = 4, fraction = 2, message = "Como máximo dos decimales.") BigDecimal precio) {
}
