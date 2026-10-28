package com.gestiondecitas.horario;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Semana de trabajo de un empleado (RN-002): cada día usa sus propios tramos si
 * ese día está ajustado y, si no, los del horario general. Los días de cierre van
 * aparte, porque dependen de la fecha y no del día de la semana.
 */
public final class HorarioSemanal {

    private static final Comparator<Tramo> POR_INICIO = Comparator.comparing(Tramo::inicio);

    private final Map<DayOfWeek, List<Tramo>> general;
    private final Map<DayOfWeek, List<Tramo>> ajustes;

    /**
     * @param general tramos del horario general; un día que no aparece está cerrado
     * @param ajustes días ajustados del empleado; una lista vacía = ese día no trabaja
     */
    public HorarioSemanal(Map<DayOfWeek, List<Tramo>> general, Map<DayOfWeek, List<Tramo>> ajustes) {
        this.general = ordenados(general);
        this.ajustes = ordenados(ajustes);
    }

    /** Solo el horario general, para quien no tiene ningún ajuste. */
    public static HorarioSemanal general(Map<DayOfWeek, List<Tramo>> general) {
        return new HorarioSemanal(general, Map.of());
    }

    /** Tramos que trabaja ese día, ordenados por hora de inicio. */
    public List<Tramo> tramos(DayOfWeek dia) {
        if (ajustes.containsKey(dia)) {
            return ajustes.get(dia);
        }
        return general.getOrDefault(dia, List.of());
    }

    public boolean ajustado(DayOfWeek dia) {
        return ajustes.containsKey(dia);
    }

    /** Días ajustados con sus tramos (la lista vacía significa que ese día no trabaja). */
    public Map<DayOfWeek, List<Tramo>> ajustes() {
        return ajustes;
    }

    private static Map<DayOfWeek, List<Tramo>> ordenados(Map<DayOfWeek, List<Tramo>> dias) {
        Map<DayOfWeek, List<Tramo>> copia = new EnumMap<>(DayOfWeek.class);
        dias.forEach((dia, tramos) -> copia.put(dia, tramos.stream().sorted(POR_INICIO).toList()));
        return copia;
    }
}
