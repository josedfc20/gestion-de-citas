package com.gestiondecitas.shared.error;

/**
 * El recurso no existe o no pertenece al usuario (paso 3.3: recursos ajenos → 404).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
