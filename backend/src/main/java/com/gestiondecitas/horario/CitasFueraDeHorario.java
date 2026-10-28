package com.gestiondecitas.horario;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.gestiondecitas.cita.CitaOcupada;
import com.gestiondecitas.cita.OcupacionesProfesional;
import com.gestiondecitas.horario.dto.CitaAfectada;

/**
 * CU-018, alt. 1: citas confirmadas futuras que, con el horario y los cierres
 * guardados ahora, caen fuera del horario de su profesional o en un día de
 * cierre. Se listan para que el administrador decida; nunca se cancelan solas.
 */
@Component
class CitasFueraDeHorario {

    private final OcupacionesProfesional ocupaciones;
    private final ConsultaHorarios consulta;
    private final Clock clock;

    CitasFueraDeHorario(OcupacionesProfesional ocupaciones, ConsultaHorarios consulta, Clock clock) {
        this.ocupaciones = ocupaciones;
        this.consulta = consulta;
        this.clock = clock;
    }

    /** Sin empleado, revisa las citas de todos. */
    List<CitaAfectada> buscar(Long empleadoId, ZoneId zona) {
        List<CitaOcupada> citas = ocupaciones.confirmadasFuturas(empleadoId, clock.instant());
        if (citas.isEmpty()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now(clock.withZone(zona));
        LocalDate ultima = citas.getLast().inicio().atZone(zona).toLocalDate();
        Map<Long, HorarioSemanal> horarios = new HashMap<>();
        Map<Long, Set<LocalDate>> cierres = new HashMap<>();
        return citas.stream()
                .filter(cita -> fuera(cita, zona,
                        horarios.computeIfAbsent(cita.empleadoId(), consulta::deEmpleado),
                        cierres.computeIfAbsent(cita.empleadoId(), id -> consulta.cierres(id, hoy, ultima))))
                .map(cita -> new CitaAfectada(cita.id(), cita.empleadoId(), cita.profesional(),
                        cita.inicio().atZone(zona).toOffsetDateTime(), cita.fin().atZone(zona).toOffsetDateTime()))
                .toList();
    }

    static boolean fuera(CitaOcupada cita, ZoneId zona, HorarioSemanal horario, Set<LocalDate> cierres) {
        ZonedDateTime inicio = cita.inicio().atZone(zona);
        ZonedDateTime fin = cita.fin().atZone(zona);
        if (cierres.contains(inicio.toLocalDate()) || !inicio.toLocalDate().equals(fin.toLocalDate())) {
            return true;
        }
        return horario.tramos(inicio.getDayOfWeek()).stream()
                .noneMatch(t -> t.contiene(inicio.toLocalTime(), fin.toLocalTime()));
    }
}
