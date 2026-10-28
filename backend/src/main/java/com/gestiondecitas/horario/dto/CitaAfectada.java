package com.gestiondecitas.horario.dto;

import java.time.OffsetDateTime;

/** Cita confirmada que queda fuera del horario o en un día de cierre (CU-018, alt. 1). Se mantiene. */
public record CitaAfectada(Long id, Long profesionalId, String profesional, OffsetDateTime inicio,
        OffsetDateTime fin) {
}
