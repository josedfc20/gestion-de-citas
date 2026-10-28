package com.gestiondecitas.shared.error;

/**
 * El dato ya existe y debe ser único, por ejemplo un correo ya registrado (→ 409).
 */
public class DatoDuplicadoException extends RuntimeException {

    public DatoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
