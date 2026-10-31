package com.gestiondecitas.shared.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * El envío real por SMTP (H-9). Las demás pruebas usan el buzón en memoria
 * ({@code CorreosDePrueba}); aquí se simula el servidor de correo con Mockito.
 */
class EnvioCorreoSmtpTest {

    @Test
    void enviaUnCorreoDeTextoConElRemitenteConfigurado() {
        JavaMailSender servidor = mock(JavaMailSender.class);
        var envio = new EnvioCorreoSmtp(servidor, "citas@negocio.example");

        envio.enviar(new Correo("ana@pruebas.local", "Restablecer tu contraseña", "Hola, Ana"));

        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(servidor).send(enviado.capture());
        assertThat(enviado.getValue().getFrom()).isEqualTo("citas@negocio.example");
        assertThat(enviado.getValue().getTo()).containsExactly("ana@pruebas.local");
        assertThat(enviado.getValue().getSubject()).isEqualTo("Restablecer tu contraseña");
        assertThat(enviado.getValue().getText()).isEqualTo("Hola, Ana");
    }
}
