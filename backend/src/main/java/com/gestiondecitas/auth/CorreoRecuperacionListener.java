package com.gestiondecitas.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.gestiondecitas.shared.correo.EnvioCorreo;

/**
 * Envía el correo de recuperación en segundo plano y solo si la transacción que
 * guardó el token se ha confirmado. Si el envío falla, se anota sin el token y
 * el usuario puede pedir otro enlace.
 */
@Component
class CorreoRecuperacionListener {

    private static final Logger log = LoggerFactory.getLogger(CorreoRecuperacionListener.class);

    private final EnvioCorreo envioCorreo;

    CorreoRecuperacionListener(EnvioCorreo envioCorreo) {
        this.envioCorreo = envioCorreo;
    }

    @Async
    @TransactionalEventListener
    void enviar(CorreoRecuperacion correo) {
        try {
            envioCorreo.enviar(correo.correo());
        } catch (RuntimeException ex) {
            log.error("No se pudo enviar el correo de recuperación de contraseña: {}", ex.getMessage());
        }
    }
}
