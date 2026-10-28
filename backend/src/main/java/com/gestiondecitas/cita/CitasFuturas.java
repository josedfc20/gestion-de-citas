package com.gestiondecitas.cita;

import java.time.Instant;

/**
 * Cuenta las citas confirmadas que aún no han empezado. La usa la gestión de
 * empleados (CU-017, alt. 2 y 3) antes de que exista la entidad {@code Cita}:
 * B6 la implementará con su repositorio sin cambiar a quien la usa.
 */
public interface CitasFuturas {

    long deEmpleado(Long empleadoId, Instant desde);

    long deEmpleadoYServicio(Long empleadoId, Long servicioId, Instant desde);
}
