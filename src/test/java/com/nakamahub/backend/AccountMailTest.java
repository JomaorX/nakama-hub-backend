package com.nakamahub.backend;

import com.nakamahub.backend.repositories.UserRepository;
import com.nakamahub.backend.security.AttemptLimiter;
import com.nakamahub.backend.support.IntegrationTest;
import com.nakamahub.backend.support.RecordingMailer;
import com.nakamahub.backend.support.TestDataFactory;
import com.nakamahub.backend.support.TestMailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import({TestDataFactory.class, TestMailConfig.class})
class AccountMailTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    @Autowired
    RecordingMailer mailer;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        data.clear();
        mailer.clear();
        for (String key : new String[] {
                "reset:ip:127.0.0.1", "registro:ip:127.0.0.1", "login:ip:127.0.0.1",
                "reset:cuenta:shanks@nakamahub.test" }) {
            limiter.reset(key);
        }
    }

    @Test
    @DisplayName("Al registrarse se envía el correo de verificación")
    void alRegistrarseSeEnviaLaVerificacion() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nuevo@nakamahub.test", "username": "recluta", "password": "Secreta1"}
                                """))
                .andExpect(status().isCreated());

        var mail = mailer.lastTo("nuevo@nakamahub.test").orElseThrow();
        assertThat(mail.subject()).contains("Confirma tu cuenta");
        assertThat(mail.body()).contains("/verificar?token=");
    }

    @Test
    @DisplayName("La cuenta nace sin verificar y el enlace la confirma")
    void elEnlaceConfirmaLaCuenta() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nuevo@nakamahub.test", "username": "recluta", "password": "Secreta1"}
                        """));

        assertThat(userRepository.findByUsername("recluta").orElseThrow().isEmailVerified()).isFalse();

        String token = mailer.tokenFrom(mailer.lastTo("nuevo@nakamahub.test").orElseThrow());

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByUsername("recluta").orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("El token de verificación no sirve dos veces")
    void elTokenDeVerificacionEsDeUnSoloUso() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nuevo@nakamahub.test", "username": "recluta", "password": "Secreta1"}
                        """));
        String token = mailer.tokenFrom(mailer.lastTo("nuevo@nakamahub.test").orElseThrow());

        mockMvc.perform(verificarCon(token)).andExpect(status().isNoContent());
        mockMvc.perform(verificarCon(token)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Pedir el restablecimiento envía el enlace")
    void pedirElRestablecimientoEnviaElEnlace() throws Exception {
        data.user("shanks");
        mailer.clear();

        mockMvc.perform(olvideCon("shanks@nakamahub.test")).andExpect(status().isNoContent());

        var mail = mailer.lastTo("shanks@nakamahub.test").orElseThrow();
        assertThat(mail.subject()).contains("Restablecer");
        assertThat(mail.body()).contains("/restablecer?token=");
    }

    @Test
    @DisplayName("Con una dirección desconocida responde igual y no envía nada")
    void conDireccionDesconocidaNoRevelaNada() throws Exception {
        mockMvc.perform(olvideCon("nadie@nakamahub.test")).andExpect(status().isNoContent());

        assertThat(mailer.all()).isEmpty();
    }

    @Test
    @DisplayName("El enlace fija la contraseña nueva y permite entrar con ella")
    void elEnlaceFijaLaContrasenaNueva() throws Exception {
        data.user("shanks");
        mailer.clear();
        mockMvc.perform(olvideCon("shanks@nakamahub.test"));

        String token = mailer.tokenFrom(mailer.lastTo("shanks@nakamahub.test").orElseThrow());

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"newPassword\": \"ClaveNueva1\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "shanks", "password": "ClaveNueva1"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Restablecer verifica la dirección de paso")
    void restablecerVerificaLaDireccion() throws Exception {
        data.user("shanks");
        mailer.clear();
        mockMvc.perform(olvideCon("shanks@nakamahub.test"));
        String token = mailer.tokenFrom(mailer.lastTo("shanks@nakamahub.test").orElseThrow());

        mockMvc.perform(post("/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + token + "\", \"newPassword\": \"ClaveNueva1\"}"));

        assertThat(userRepository.findByUsername("shanks").orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("Pedir el enlace dos veces invalida el anterior")
    void pedirloDosVecesInvalidaElAnterior() throws Exception {
        data.user("shanks");
        mailer.clear();

        mockMvc.perform(olvideCon("shanks@nakamahub.test"));
        String primero = mailer.tokenFrom(mailer.lastTo("shanks@nakamahub.test").orElseThrow());

        mockMvc.perform(olvideCon("shanks@nakamahub.test"));

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + primero + "\", \"newPassword\": \"ClaveNueva1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un token inventado no restablece nada")
    void unTokenInventadoNoVale() throws Exception {
        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"me-lo-invento\", \"newPassword\": \"ClaveNueva1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El formulario de recuperación tiene límite de envíos")
    void elFormularioDeRecuperacionTieneLimite() throws Exception {
        data.user("shanks");

        for (int intento = 0; intento < 3; intento++) {
            mockMvc.perform(olvideCon("shanks@nakamahub.test")).andExpect(status().isNoContent());
        }

        mockMvc.perform(olvideCon("shanks@nakamahub.test"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("El perfil propio informa de si la dirección está verificada")
    void elPerfilInformaDeLaVerificacion() throws Exception {
        data.user("shanks");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("shanks")))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    private RequestBuilder olvideCon(String email) {
        return post("/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\"}");
    }

    private RequestBuilder verificarCon(String token) {
        return post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + token + "\"}");
    }
}
