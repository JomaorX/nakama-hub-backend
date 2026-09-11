package com.nakamahub.backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailer(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // No se propaga: que el proveedor de correo falle no debe tumbar el
            // registro ni la petición de restablecimiento. Queda en el log para
            // poder reenviar, y el usuario siempre puede volver a pedir el enlace.
            log.error("No se ha podido enviar el correo a {} con asunto '{}'", to, subject, ex);
        }
    }
}
