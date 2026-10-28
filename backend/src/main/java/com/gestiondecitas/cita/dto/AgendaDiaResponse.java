package com.gestiondecitas.cita.dto;

import java.time.LocalDate;
import java.util.List;

import com.gestiondecitas.horario.dto.TramoDto;

/**
 * Un día de la agenda del empleado (CU-013): su horario efectivo y sus citas no
 * canceladas. Sin tramos (o con {@code cierre}) es un día no laborable.
 */
public record AgendaDiaResponse(LocalDate fecha, boolean cierre, List<TramoDto> tramos, List<CitaResponse> citas) {
}
