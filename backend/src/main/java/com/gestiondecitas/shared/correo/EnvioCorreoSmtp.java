package com.gestiondecitas.shared.correo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envía por SMTP ({@code spring.mail.*}). En local, a Mailpit (http://localhost:8025). */
@Component
public class EnvioCorreoSmtp implements EnvioCorreo {

    private final JavaMailSender mailSender;
    private final String remitente;

    public EnvioCorreoSmtp(JavaMailSender mailSender, @Value("${app.correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(Correo correo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correo.para());
        mensaje.setSubject(correo.asunto());
        mensaje.setText(correo.texto());
        mailSender.send(mensaje);
    }
}
