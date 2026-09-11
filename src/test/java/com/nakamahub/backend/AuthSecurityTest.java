package com.nakamahub.backend;

import com.nakamahub.backend.models.UserRole;
import com.nakamahub.backend.security.JwtUtil;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(TestDataFactory.class)
class AuthSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    @Autowired
    JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        data.clear();
    }

    @Test
    @DisplayName("El login con contraseña incorrecta devuelve 401, no 400")
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        data.user("hinata");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "hinata", "password": "NoEsLaBuena1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("El login correcto devuelve un token utilizable")
    void loginCorrectoDevuelveToken() throws Exception {
        data.user("luffy");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "luffy", "password": "%s"}
                                """.formatted(TestDataFactory.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Una petición sin token a un recurso protegido devuelve 401, no 403")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Un token manipulado devuelve 401 en lugar de romper con un 500")
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un usuario normal no puede usar los endpoints de moderación")
    void usuarioNormalNoPuedeModerar() throws Exception {
        data.user("zoro");
        data.user("sanji");

        mockMvc.perform(put("/api/users/sanji/suspend")
                        .header(HttpHeaders.AUTHORIZATION, bearer("zoro", UserRole.ROLE_USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Un moderador sí puede suspender una cuenta")
    void moderadorPuedeSuspender() throws Exception {
        data.user("garp", UserRole.ROLE_MODERATOR);
        data.user("buggy");

        mockMvc.perform(put("/api/users/buggy/suspend")
                        .header(HttpHeaders.AUTHORIZATION, bearer("garp", UserRole.ROLE_MODERATOR)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Una cuenta suspendida deja de poder usar la API")
    void cuentaSuspendidaNoPuedeOperar() throws Exception {
        data.user("garp", UserRole.ROLE_MODERATOR);
        data.user("buggy");

        mockMvc.perform(put("/api/users/buggy/suspend")
                .header(HttpHeaders.AUTHORIZATION, bearer("garp", UserRole.ROLE_MODERATOR)));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer("buggy", UserRole.ROLE_USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tu cuenta está suspendida"));
    }

    @Test
    @DisplayName("Los errores de validación devuelven 400 con el detalle por campo")
    void erroresDeValidacionDevuelvenDetallePorCampo() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "no-es-un-email", "username": "ab", "password": "corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    private String bearer(String username, UserRole role) {
        return "Bearer " + jwtUtil.generateToken(username, role.name());
    }
}
