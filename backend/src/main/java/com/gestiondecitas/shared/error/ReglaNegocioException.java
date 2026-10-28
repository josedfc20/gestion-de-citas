package com.gestiondecitas.shared.error;

/**
 * La petición tiene un formato correcto pero incumple una regla de negocio,
 * por ejemplo cancelar fuera de plazo (RN-004 → 422).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
