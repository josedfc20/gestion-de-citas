package com.gestiondecitas.cita.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

/** CU-019, alt. 1: el administrador reserva en nombre de un cliente registrado. */
public record AdminReservaRequest(
        @NotNull Long clienteId,
        @NotNull Long servicioId,
        @NotNull Long profesionalId,
        @NotNull OffsetDateTime inicio,
        Long beneficiarioId) {

    public ReservaRequest reserva() {
        return new ReservaRequest(servicioId, profesionalId, inicio, beneficiarioId);
    }
}
