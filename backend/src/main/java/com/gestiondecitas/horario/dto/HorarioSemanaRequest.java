package com.gestiondecitas.horario.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Semana de horario (CU-018). En el horario general, un día que no aparece queda
 * cerrado; en el de un empleado, sigue el horario general.
 */
public record HorarioSemanaRequest(@NotNull @Size(max = 7) List<@Valid @NotNull DiaHorarioDto> dias) {

    @JsonIgnore
    @AssertTrue(message = "Cada día de la semana solo puede aparecer una vez.")
    public boolean isSinDiasRepetidos() {
        if (dias == null) {
            return true;
        }
        List<Integer> numeros = dias.stream().filter(Objects::nonNull).map(DiaHorarioDto::diaSemana)
                .filter(Objects::nonNull).toList();
        return new HashSet<>(numeros).size() == numeros.size();
    }
}
