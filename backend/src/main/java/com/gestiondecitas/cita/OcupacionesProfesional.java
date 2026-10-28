package com.gestiondecitas.cita;

import java.time.Instant;
import java.util.List;

/**
 * Citas que ocupan la agenda de los profesionales. La usan la disponibilidad
 * (CU-007) y los horarios (CU-018, alt. 1) antes de que exista la entidad
 * {@code Cita}: B6 la implementará con su repositorio sin cambiar a quien la usa.
 */
public interface OcupacionesProfesional {

    /**
     * Citas que ocupan al empleado y se solapan con [desde, hasta): las confirmadas y
     * las completadas, los mismos estados que la restricción {@code no_solape_profesional}.
     */
    List<CitaOcupada> deEmpleado(Long empleadoId, Instant desde, Instant hasta);

    /** Citas confirmadas que empiezan después de {@code desde}; sin empleado, las de todos. */
    List<CitaOcupada> confirmadasFuturas(Long empleadoId, Instant desde);
}
