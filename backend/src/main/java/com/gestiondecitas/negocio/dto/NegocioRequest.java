package com.gestiondecitas.negocio.dto;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Configuración del negocio que cambia el administrador ({@code PUT /admin/negocio}). */
public record NegocioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 50) String zonaHoraria,
        /** RN-010. */
        @NotNull Integer intervaloMinutos,
        /** RN-009. */
        @NotNull @Min(1) @Max(365) Integer antelacionMaxDias,
        /** RN-004: hasta una semana. */
        @NotNull @Min(0) @Max(168) Integer horasMinCancelacion,
        /** RN-006: por debajo de 14 años la LOPDGDD (art. 7) no admite el consentimiento propio. */
        @NotNull @Min(14) @Max(18) Integer edadMinimaCuenta) {

    /** Divisores de 60: los huecos siempre caen en horas "redondas". */
    public static final Set<Integer> INTERVALOS_PERMITIDOS = Set.of(5, 10, 15, 20, 30, 60);

    @JsonIgnore
    @AssertTrue(message = "El intervalo debe ser 5, 10, 15, 20, 30 o 60 minutos.")
    public boolean isIntervaloMinutosValido() {
        return intervaloMinutos == null || INTERVALOS_PERMITIDOS.contains(intervaloMinutos);
    }

    @JsonIgnore
    @AssertTrue(message = "Zona horaria no válida (por ejemplo, Europe/Madrid).")
    public boolean isZonaHorariaValida() {
        if (zonaHoraria == null || zonaHoraria.isBlank()) {
            return true; // ya lo indica @NotBlank
        }
        try {
            ZoneId.of(zonaHoraria.trim());
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }
}
