package com.nakamahub.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Sustituye el envío real por el grabador durante los tests.
 *
 * RecordingMailer ya implementa Mailer, así que basta un único bean marcado como
 * preferente; declarar además otro del tipo de la interfaz dejaba dos candidatos
 * preferentes y el contexto no arrancaba.
 */
@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    RecordingMailer recordingMailer() {
        return new RecordingMailer();
    }
}
