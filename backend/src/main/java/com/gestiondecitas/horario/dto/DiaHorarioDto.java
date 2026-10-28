package com.gestiondecitas.horario.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un día de la semana (1 = lunes ... 7 = domingo) con sus tramos; sin tramos, cerrado o libre. */
public record DiaHorarioDto(
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        @NotNull @Size(max = 4, message = "Como máximo 4 tramos por día.") List<@Valid @NotNull TramoDto> tramos) {
}
