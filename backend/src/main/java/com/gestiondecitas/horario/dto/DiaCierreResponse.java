package com.gestiondecitas.horario.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Día de cierre. {@code empleadoId} y {@code empleado} van vacíos si cierra todo
 * el negocio; {@code citasAfectadas} solo aparece al crearlo (CU-018, alt. 1).
 */
public record DiaCierreResponse(Long id, LocalDate fecha, Long empleadoId, String empleado, String motivo,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<CitaAfectada> citasAfectadas) {
}
