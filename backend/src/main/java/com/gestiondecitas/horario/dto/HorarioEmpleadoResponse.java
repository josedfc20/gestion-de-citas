package com.gestiondecitas.horario.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Horario de un empleado (CU-018, RN-002): sus días ajustados y la semana que
 * resulta de mezclarlos con el horario general. {@code citasAfectadas} solo
 * aparece al guardarlo.
 */
public record HorarioEmpleadoResponse(Long empleadoId, List<DiaHorarioDto> ajustes,
        List<DiaEfectivoDto> semanaEfectiva,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<CitaAfectada> citasAfectadas) {
}
