package com.gestiondecitas.cita;

/** RN-008 y diagrama {@code Estados-Cita}. Solo CONFIRMADA admite cambios. */
public enum EstadoCita {
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_PRESENTADO;

    /** Los estados que ocupan al profesional (restricción {@code no_solape_profesional}). */
    public boolean ocupa() {
        return this == CONFIRMADA || this == COMPLETADA;
    }
}
