package com.gestiondecitas.horario;

import java.time.LocalTime;

/** Franja de trabajo de un día, de {@code inicio} (incluido) a {@code fin} (excluido). */
public record Tramo(LocalTime inicio, LocalTime fin) {

    public Tramo {
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("El inicio del tramo debe ser anterior al fin.");
        }
    }

    /** Se solapan o se tocan (09:00-14:00 y 14:00-20:00). */
    boolean chocaCon(Tramo otro) {
        return !inicio.isAfter(otro.fin) && !otro.inicio.isAfter(fin);
    }

    /** La franja de {@code desde} a {@code hasta} cabe entera en el tramo. */
    public boolean contiene(LocalTime desde, LocalTime hasta) {
        return !desde.isBefore(inicio) && !hasta.isAfter(fin) && desde.isBefore(hasta);
    }

    @Override
    public String toString() {
        return inicio + "-" + fin;
    }
}
