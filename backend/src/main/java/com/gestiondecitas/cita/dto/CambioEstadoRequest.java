package com.gestiondecitas.cita.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.gestiondecitas.cita.EstadoCita;

/** CU-015 y CU-019. El motivo es obligatorio para cancelar (lo comprueba la cita: 422). */
public record CambioEstadoRequest(@NotNull EstadoCita estado, @Size(max = 300) String motivo) {
}
