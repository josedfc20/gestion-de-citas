package com.gestiondecitas.shared.error;

/**
 * El hueco ya está ocupado por otra cita del mismo profesional (RNF-003 → 409).
 */
public class HuecoNoDisponibleException extends RuntimeException {

    public HuecoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
