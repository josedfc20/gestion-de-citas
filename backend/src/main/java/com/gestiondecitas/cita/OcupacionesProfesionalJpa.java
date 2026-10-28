package com.gestiondecitas.cita;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/** Sustituye a la consulta provisional del B5 (pendiente cerrado en el B6). */
@Component
class OcupacionesProfesionalJpa implements OcupacionesProfesional {

    static final Set<EstadoCita> QUE_OCUPAN = Set.of(EstadoCita.CONFIRMADA, EstadoCita.COMPLETADA);

    private final CitaRepository citas;

    OcupacionesProfesionalJpa(CitaRepository citas) {
        this.citas = citas;
    }

    @Override
    public List<CitaOcupada> deEmpleado(Long empleadoId, Instant desde, Instant hasta) {
        return citas.solapadasDeEmpleado(empleadoId, desde, hasta, QUE_OCUPAN).stream()
                .map(OcupacionesProfesionalJpa::ocupada)
                .toList();
    }

    @Override
    public List<CitaOcupada> confirmadasFuturas(Long empleadoId, Instant desde) {
        return citas.confirmadasFuturas(empleadoId, desde).stream()
                .map(OcupacionesProfesionalJpa::ocupada)
                .toList();
    }

    private static CitaOcupada ocupada(Cita cita) {
        String profesional = (cita.getEmpleado().getNombre() + " " + cita.getEmpleado().getApellidos()).trim();
        return new CitaOcupada(cita.getId(), cita.getEmpleado().getId(), profesional, cita.getInicio(),
                cita.getFin());
    }
}
