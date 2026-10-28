package com.gestiondecitas.horario;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gestiondecitas.shared.error.ReglaNegocioException;

/**
 * CU-018, paso 4: los tramos de un mismo día no se solapan ni se tocan (dos
 * tramos seguidos deben ser uno solo, para que un servicio no quede partido).
 * El formato (inicio anterior al fin, minutos...) ya lo comprueban los DTO.
 */
final class ValidadorTramos {

    private static final Locale ES = Locale.of("es", "ES");

    private ValidadorTramos() {
    }

    static void comprobar(Map<DayOfWeek, List<Tramo>> dias) {
        dias.keySet().stream().sorted().forEach(dia -> {
            List<Tramo> tramos = dias.get(dia).stream().sorted(Comparator.comparing(Tramo::inicio)).toList();
            for (int i = 1; i < tramos.size(); i++) {
                Tramo anterior = tramos.get(i - 1);
                Tramo actual = tramos.get(i);
                if (anterior.chocaCon(actual)) {
                    String problema = anterior.fin().equals(actual.inicio())
                            ? " se tocan (" + anterior + " y " + actual + "): únelos en uno."
                            : " se solapan (" + anterior + " y " + actual + ").";
                    throw new ReglaNegocioException("Los tramos del " + nombre(dia) + problema);
                }
            }
        });
    }

    static String nombre(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, ES);
    }
}
