package com.nakamahub.backend.config;

import com.nakamahub.backend.mail.LoggingMailer;
import com.nakamahub.backend.mail.Mailer;
import com.nakamahub.backend.mail.SmtpMailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Elige el mecanismo de envío según haya o no servidor SMTP configurado.
 *
 * El orden de los métodos importa: las condiciones sobre beans ausentes se evalúan
 * en el orden de declaración, así que el de SMTP tiene que ir primero para que el
 * de respaldo solo entre cuando el otro no se ha creado.
 */
@Configuration
public class MailConfig {

    @Bean
    @Conditional(SmtpConfiguredCondition.class)
    Mailer smtpMailer(JavaMailSender mailSender,
                      @Value("${app.mail.from:no-responder@nakamahub.example}") String from) {
        return new SmtpMailer(mailSender, from);
    }

    @Bean
    @ConditionalOnMissingBean(Mailer.class)
    Mailer loggingMailer() {
        return new LoggingMailer();
    }
}
