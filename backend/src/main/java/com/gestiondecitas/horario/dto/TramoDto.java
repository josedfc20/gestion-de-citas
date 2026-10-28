package com.gestiondecitas.horario.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.gestiondecitas.horario.Tramo;

/** Un tramo en la API: {@code {"inicio": "09:00", "fin": "14:00"}}. */
public record TramoDto(
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime inicio,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime fin) {

    public static TramoDto de(Tramo tramo) {
        return new TramoDto(tramo.inicio(), tramo.fin());
    }

    public Tramo aTramo() {
        return new Tramo(inicio, fin);
    }

    @JsonIgnore
    @AssertTrue(message = "La hora de inicio debe ser anterior a la de fin.")
    public boolean isOrdenado() {
        return inicio == null || fin == null || inicio.isBefore(fin);
    }

    @JsonIgnore
    @AssertTrue(message = "Las horas van en múltiplos de 5 minutos (09:00, 09:05...).")
    public boolean isEnMultiplosDe5() {
        return (inicio == null || inicio.getMinute() % 5 == 0) && (fin == null || fin.getMinute() % 5 == 0);
    }
}
