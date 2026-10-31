package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;

import com.gestiondecitas.shared.correo.Correo;
import com.gestiondecitas.shared.correo.EnvioCorreo;

/**
 * Si el servidor de correo falla (H-9), el error se anota sin el token y no se
 * propaga: la respuesta de "recuperar contraseña" ya se dio y el usuario puede
 * pedir otro enlace.
 */
@ExtendWith(OutputCaptureExtension.class)
class CorreoRecuperacionListenerTest {

    private static final String TOKEN = "token-secreto-de-prueba";

    @Test
    void unFalloAlEnviarSeAnotaSinElTokenYNoSePropaga(CapturedOutput salida) {
        EnvioCorreo envio = mock(EnvioCorreo.class);
        doThrow(new MailSendException("Servidor SMTP no disponible")).when(envio).enviar(any(Correo.class));
        var listener = new CorreoRecuperacionListener(envio);
        var correo = new CorreoRecuperacion("ana@pruebas.local", "Ana", "Peluquería Demo",
                CorreoRecuperacion.enlace("http://localhost:5173", TOKEN));

        assertThatCode(() -> listener.enviar(correo)).doesNotThrowAnyException();

        assertThat(salida.getOut()).contains("No se pudo enviar el correo de recuperación de contraseña")
                .contains("Servidor SMTP no disponible")
                .doesNotContain(TOKEN);
    }
}
