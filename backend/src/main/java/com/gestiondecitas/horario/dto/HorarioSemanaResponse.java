package com.gestiondecitas.horario.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Horario general: los 7 días, de lunes a domingo. {@code citasAfectadas} solo
 * aparece al guardarlo (CU-018, alt. 1).
 */
public record HorarioSemanaResponse(List<DiaHorarioDto> dias,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<CitaAfectada> citasAfectadas) {
}
