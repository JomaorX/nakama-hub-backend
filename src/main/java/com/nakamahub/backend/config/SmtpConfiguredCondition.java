package com.nakamahub.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Cierta solo cuando hay un servidor SMTP de verdad configurado.
 *
 * No vale con @ConditionalOnProperty: el fichero de propiedades define
 * spring.mail.host con valor por defecto vacío, y para esa anotación una propiedad
 * presente pero vacía cuenta como presente. El resultado era que en desarrollo se
 * elegía el envío por SMTP y cada registro intentaba conectar a un servidor que no
 * existe.
 */
public class SmtpConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String host = context.getEnvironment().getProperty("spring.mail.host");
        return host != null && !host.isBlank();
    }
}
