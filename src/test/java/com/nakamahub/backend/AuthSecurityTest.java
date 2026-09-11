package com.nakamahub.backend;

import com.nakamahub.backend.models.UserRole;
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
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
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
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("zoro")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Un moderador sí puede suspender una cuenta")
    void moderadorPuedeSuspender() throws Exception {
        data.user("garp", UserRole.ROLE_MODERATOR);
        data.user("buggy");

        mockMvc.perform(put("/api/users/buggy/suspend")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Una cuenta suspendida deja de poder usar la API")
    void cuentaSuspendidaNoPuedeOperar() throws Exception {
        data.user("garp", UserRole.ROLE_MODERATOR);
        data.user("buggy");

        mockMvc.perform(put("/api/users/buggy/suspend")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("buggy")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tu cuenta está suspendida"));
    }

    @Test
    @DisplayName("Cambiar el nombre de usuario invalida los tokens anteriores")
    void cambiarNombreInvalidaElTokenAnterior() throws Exception {
        data.user("chopper");
        String tokenAntiguo = data.bearer("chopper");

        mockMvc.perform(put("/api/users/me/username")
                        .header(HttpHeaders.AUTHORIZATION, tokenAntiguo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "tonytony"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, tokenAntiguo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token no sirve si otra persona ocupa después el nombre liberado")
    void tokenNoSirveSiOtroOcupaElNombre() throws Exception {
        data.user("franky");
        String tokenDeFranky = data.bearer("franky");

        mockMvc.perform(put("/api/users/me/username")
                .header(HttpHeaders.AUTHORIZATION, tokenDeFranky)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "cutty"}
                        """));

        // Otra persona registra el nombre que acaba de quedar libre.
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "impostor@nakamahub.test", "username": "franky", "password": "Secreta1"}
                                """))
                .andExpect(status().isCreated());

        // El token antiguo dice "franky", pero ese nombre ya es de otra cuenta.
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, tokenDeFranky))
                .andExpect(status().isUnauthorized());
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
}
