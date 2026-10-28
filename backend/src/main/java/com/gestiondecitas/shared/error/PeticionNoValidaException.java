package com.gestiondecitas.shared.error;

/**
 * Parámetros con un formato válido que juntos no tienen sentido, por ejemplo un
 * rango de fechas al revés o demasiado largo (→ 400).
 */
public class PeticionNoValidaException extends RuntimeException {

    public PeticionNoValidaException(String mensaje) {
        super(mensaje);
    }
}
