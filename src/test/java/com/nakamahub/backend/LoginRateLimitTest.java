package com.nakamahub.backend;

import com.nakamahub.backend.security.AttemptLimiter;
import com.nakamahub.backend.support.IntegrationTest;
import com.nakamahub.backend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(TestDataFactory.class)
class LoginRateLimitTest {

    private static final int MAX_PER_ACCOUNT = 5;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    /** El recuento vive en memoria y es compartido, así que hay que vaciarlo entre pruebas. */
    @Autowired
    AttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        data.clear();
        data.user("ace");
        data.user("sabo");
        for (String key : new String[] {
                "login:cuenta:ace", "login:cuenta:sabo", "login:ip:127.0.0.1", "registro:ip:127.0.0.1" }) {
            limiter.reset(key);
        }
    }

    @Test
    @DisplayName("Tras agotar los intentos el login responde 429 con Retry-After")
    void trasAgotarLosIntentosResponde429() throws Exception {
        for (int intento = 0; intento < MAX_PER_ACCOUNT; intento++) {
            mockMvc.perform(loginCon("ace", "NoEsLaBuena1"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginCon("ace", "NoEsLaBuena1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("El bloqueo se aplica aunque después se acierte la contraseña")
    void elBloqueoSeAplicaAunqueSeAcierte() throws Exception {
        for (int intento = 0; intento < MAX_PER_ACCOUNT; intento++) {
            mockMvc.perform(loginCon("ace", "NoEsLaBuena1"));
        }

        mockMvc.perform(loginCon("ace", TestDataFactory.PASSWORD))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Variar las mayúsculas del usuario no esquiva el límite")
    void variarLasMayusculasNoEsquivaElLimite() throws Exception {
        for (int intento = 0; intento < MAX_PER_ACCOUNT; intento++) {
            mockMvc.perform(loginCon("ace", "NoEsLaBuena1"));
        }

        mockMvc.perform(loginCon("ACE", "NoEsLaBuena1"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Un login correcto limpia el recuento de fallos")
    void unLoginCorrectoLimpiaElRecuento() throws Exception {
        for (int intento = 0; intento < MAX_PER_ACCOUNT - 1; intento++) {
            mockMvc.perform(loginCon("sabo", "NoEsLaBuena1"));
        }

        mockMvc.perform(loginCon("sabo", TestDataFactory.PASSWORD))
                .andExpect(status().isOk());

        // Si el recuento no se hubiera limpiado, este fallo sería el quinto y bloquearía.
        mockMvc.perform(loginCon("sabo", "NoEsLaBuena1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bloquear una cuenta no bloquea a las demás")
    void bloquearUnaCuentaNoBloqueaLasDemas() throws Exception {
        for (int intento = 0; intento < MAX_PER_ACCOUNT; intento++) {
            mockMvc.perform(loginCon("ace", "NoEsLaBuena1"));
        }

        mockMvc.perform(loginCon("ace", "NoEsLaBuena1")).andExpect(status().isTooManyRequests());
        mockMvc.perform(loginCon("sabo", TestDataFactory.PASSWORD)).andExpect(status().isOk());
    }

    private RequestBuilder loginCon(String identifier, String password) {
        return post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\": \"" + identifier + "\", \"password\": \"" + password + "\"}");
    }
}
