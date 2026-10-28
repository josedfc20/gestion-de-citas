package com.gestiondecitas.horario;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee de la base de datos el horario de un empleado y sus días de cierre. La usan
 * la disponibilidad (CU-007) y la gestión de horarios (CU-018).
 */
@Component
public class ConsultaHorarios {

    private final TramoHorarioRepository tramos;
    private final AjusteHorarioRepository ajustes;
    private final DiaCierreRepository diasCierre;

    public ConsultaHorarios(TramoHorarioRepository tramos, AjusteHorarioRepository ajustes,
            DiaCierreRepository diasCierre) {
        this.tramos = tramos;
        this.ajustes = ajustes;
        this.diasCierre = diasCierre;
    }

    /** Tramos del horario general por día; un día que no aparece está cerrado. */
    @Transactional(readOnly = true)
    public Map<DayOfWeek, List<Tramo>> general() {
        return porDia(tramos.findByEmpleadoIdIsNull());
    }

    /** RN-002: el horario general con los días que el empleado tiene ajustados. */
    @Transactional(readOnly = true)
    public HorarioSemanal deEmpleado(Long empleadoId) {
        Map<DayOfWeek, List<Tramo>> suyos = porDia(tramos.findByEmpleadoId(empleadoId));
        Map<DayOfWeek, List<Tramo>> ajustados = new EnumMap<>(DayOfWeek.class);
        ajustes.findByEmpleadoId(empleadoId)
                .forEach(a -> ajustados.put(a.getDia(), suyos.getOrDefault(a.getDia(), List.of())));
        return new HorarioSemanal(general(), ajustados);
    }

    /** Fechas entre {@code desde} y {@code hasta} (incluidas) en que cierra el negocio o el empleado. */
    @Transactional(readOnly = true)
    public Set<LocalDate> cierres(Long empleadoId, LocalDate desde, LocalDate hasta) {
        return diasCierre.queAfectanA(empleadoId, desde, hasta).stream()
                .map(DiaCierre::getFecha)
                .collect(Collectors.toSet());
    }

    private static Map<DayOfWeek, List<Tramo>> porDia(List<TramoHorario> filas) {
        return filas.stream().collect(Collectors.groupingBy(TramoHorario::getDia,
                () -> new EnumMap<>(DayOfWeek.class),
                Collectors.mapping(TramoHorario::getTramo, Collectors.toList())));
    }
}
