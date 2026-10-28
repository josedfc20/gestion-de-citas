package com.gestiondecitas.empleado.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

/** Lista completa de servicios que realiza el empleado: reemplaza la anterior. */
public record ServiciosAsignadosRequest(@NotNull Set<@NotNull Long> servicioIds) {
}
