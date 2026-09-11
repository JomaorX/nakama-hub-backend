package com.nakamahub.backend.services;

import com.nakamahub.backend.mail.Mailer;
import com.nakamahub.backend.models.TokenPurpose;
import com.nakamahub.backend.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Los dos correos que la plataforma envía: confirmar la dirección y recuperar la cuenta. */
@Service
@Transactional
public class AccountMailService {

    private final OneTimeTokenService tokenService;
    private final Mailer mailer;
    private final String siteOrigin;

    public AccountMailService(OneTimeTokenService tokenService,
                              Mailer mailer,
                              @Value("${app.site-origin}") String siteOrigin) {
        this.tokenService = tokenService;
        this.mailer = mailer;
        this.siteOrigin = siteOrigin.replaceAll("/+$", "");
    }

    public void sendVerification(User user) {
        String token = tokenService.issue(user, TokenPurpose.VERIFICACION_EMAIL);
        String link = siteOrigin + "/verificar?token=" + encode(token);

        mailer.send(user.getEmail(), "Confirma tu cuenta en Nakama Hub", """
                Hola %s:

                Para terminar de crear tu cuenta, confirma que esta dirección es tuya:

                %s

                El enlace caduca en dos días. Si no te has registrado en Nakama Hub,
                puedes ignorar este mensaje.
                """.formatted(user.getUsername(), link));
    }

    public void sendPasswordReset(User user) {
        String token = tokenService.issue(user, TokenPurpose.RESTABLECER_PASSWORD);
        String link = siteOrigin + "/restablecer?token=" + encode(token);

        mailer.send(user.getEmail(), "Restablecer tu contraseña de Nakama Hub", """
                Hola %s:

                Alguien ha pedido restablecer la contraseña de tu cuenta. Si has sido tú,
                elige una nueva aquí:

                %s

                El enlace caduca en una hora y solo se puede usar una vez. Si no has sido
                tú, ignora este mensaje: tu contraseña actual sigue siendo válida.
                """.formatted(user.getUsername(), link));
    }

    private String encode(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
