package com.nakamahub.backend.support;

import com.nakamahub.backend.mail.Mailer;
import org.springframework.boot.test.context.TestComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Guarda los correos en memoria en lugar de enviarlos, para poder comprobar en los
 * tests qué se envió y extraer el enlace sin depender de un servidor SMTP.
 */
@TestComponent
public class RecordingMailer implements Mailer {

    public record SentMail(String to, String subject, String body) {
    }

    private final List<SentMail> sent = new ArrayList<>();

    @Override
    public synchronized void send(String to, String subject, String body) {
        sent.add(new SentMail(to, subject, body));
    }

    public synchronized void clear() {
        sent.clear();
    }

    public synchronized List<SentMail> all() {
        return List.copyOf(sent);
    }

    public synchronized Optional<SentMail> lastTo(String address) {
        return sent.stream().filter(mail -> mail.to().equals(address)).reduce((first, last) -> last);
    }

    /** Extrae el token del enlace que lleva el cuerpo del correo. */
    public String tokenFrom(SentMail mail) {
        int start = mail.body().indexOf("token=");
        if (start < 0) {
            throw new IllegalStateException("El correo no lleva enlace con token:\n" + mail.body());
        }
        String rest = mail.body().substring(start + "token=".length());
        int end = rest.indexOf('\n');
        return (end < 0 ? rest : rest.substring(0, end)).trim();
    }
}
