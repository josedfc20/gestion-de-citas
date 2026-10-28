package com.gestiondecitas.cita.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Reservar (CU-008). La hora viene de {@code GET /disponibilidad}; el fin y el
 * precio los calcula el servidor. Sin {@code beneficiarioId}, la cita es para el
 * propio cliente (CU-009).
 */
public record ReservaRequest(
        @NotNull Long servicioId,
        @NotNull Long profesionalId,
        @NotNull OffsetDateTime inicio,
        Long beneficiarioId) {
}
