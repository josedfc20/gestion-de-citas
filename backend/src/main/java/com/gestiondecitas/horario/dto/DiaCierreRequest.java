package com.gestiondecitas.horario.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Día de cierre (CU-018). Sin {@code empleadoId} cierra todo el negocio. */
public record DiaCierreRequest(@NotNull LocalDate fecha, Long empleadoId, @Size(max = 200) String motivo) {
}
