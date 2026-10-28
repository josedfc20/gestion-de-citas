package com.gestiondecitas.shared.error;

/**
 * Credenciales o token de refresco no válidos (→ 401). El mensaje se envía al
 * cliente, así que nunca debe revelar qué parte ha fallado.
 */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
