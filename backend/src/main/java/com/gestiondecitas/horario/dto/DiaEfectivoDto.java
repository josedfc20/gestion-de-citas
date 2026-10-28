package com.gestiondecitas.horario.dto;

import java.util.List;

/** Lo que un empleado trabaja de verdad un día: sus tramos si está {@code ajustado} o los del general. */
public record DiaEfectivoDto(int diaSemana, boolean ajustado, List<TramoDto> tramos) {
}
