package com.nakamahub.backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe el correo en el log en lugar de enviarlo.
 *
 * Es lo que se usa mientras no haya credenciales SMTP configuradas. Permite seguir
 * el enlace de verificación o de restablecimiento desde la consola y probar el
 * flujo entero sin depender de un proveedor.
 */
public class LoggingMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailer.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ─── Correo no enviado, no hay SMTP configurado ───
                Para:    {}
                Asunto:  {}

                {}
                ─────────────────────────────────────────────────
                """, to, subject, body);
    }
}
