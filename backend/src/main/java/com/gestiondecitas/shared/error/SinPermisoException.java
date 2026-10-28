package com.gestiondecitas.shared.error;

/**
 * El usuario se ha identificado pero no puede hacer la operación, por ejemplo
 * porque su cuenta está desactivada (→ 403).
 */
public class SinPermisoException extends RuntimeException {

    public SinPermisoException(String mensaje) {
        super(mensaje);
    }
}
