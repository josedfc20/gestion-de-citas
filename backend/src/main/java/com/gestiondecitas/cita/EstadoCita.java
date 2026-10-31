package com.gestiondecitas.cita;

/** RN-008 y diagrama {@code Estados-Cita}. Solo CONFIRMADA admite cambios. */
public enum EstadoCita {
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_PRESENTADO
}
