package com.gestiondecitas.cita;

import java.time.Instant;

import org.springframework.stereotype.Component;

/** Sustituye a la consulta provisional del B4 (pendiente cerrado en el B6). */
@Component
class CitasFuturasJpa implements CitasFuturas {

    private final CitaRepository citas;

    CitasFuturasJpa(CitaRepository citas) {
        this.citas = citas;
    }

    @Override
    public long deEmpleado(Long empleadoId, Instant desde) {
        return citas.countByEmpleadoIdAndEstadoAndInicioAfter(empleadoId, EstadoCita.CONFIRMADA, desde);
    }

    @Override
    public long deEmpleadoYServicio(Long empleadoId, Long servicioId, Instant desde) {
        return citas.countByEmpleadoIdAndServicioIdAndEstadoAndInicioAfter(empleadoId, servicioId,
                EstadoCita.CONFIRMADA, desde);
    }
}
