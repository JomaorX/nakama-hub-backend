package com.nakamahub.backend.mail;

/** Envío de correo, abstraído para poder arrancar sin servidor SMTP en desarrollo. */
public interface Mailer {

    void send(String to, String subject, String body);
}
