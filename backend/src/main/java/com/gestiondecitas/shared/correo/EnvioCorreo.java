package com.gestiondecitas.shared.correo;

/**
 * Envío de correos (paso 4.3). Detrás de una interfaz para que las pruebas usen
 * una implementación en memoria y el proveedor real se elija en la fase 7.
 */
public interface EnvioCorreo {

    void enviar(Correo correo);
}
