package com.gestiondecitas.shared.dto;

import jakarta.validation.constraints.NotNull;

/** Activar o desactivar un recurso ({@code PATCH .../activo}). */
public record CambioActivoRequest(@NotNull Boolean activo) {
}
