package com.nakamahub.backend;

import com.jayway.jsonpath.JsonPath;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(TestDataFactory.class)
class SessionLifecycleTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    @BeforeEach
    void setUp() {
        data.clear();
        data.user("jinbe");
    }

    @Test
    @DisplayName("El login entrega token de acceso y token de refresco")
    void elLoginEntregaAmbosTokens() throws Exception {
        Sesion sesion = login("jinbe");

        assertThat(sesion.accessToken()).isNotBlank();
        assertThat(sesion.refreshToken()).isNotBlank();
        assertThat(sesion.accessToken()).isNotEqualTo(sesion.refreshToken());
    }

    @Test
    @DisplayName("El refresco devuelve un par nuevo y el acceso resultante funciona")
    void elRefrescoDevuelveUnParNuevoYUtilizable() throws Exception {
        Sesion inicial = login("jinbe");
        Sesion renovada = refrescar(inicial.refreshToken());

        assertThat(renovada.refreshToken()).isNotEqualTo(inicial.refreshToken());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + renovada.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jinbe"));
    }

    @Test
    @DisplayName("Un token de refresco ya canjeado no vuelve a servir")
    void elTokenDeRefrescoEsDeUnSoloUso() throws Exception {
        Sesion inicial = login("jinbe");
        refrescar(inicial.refreshToken());

        mockMvc.perform(refrescoCon(inicial.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reutilizar un token gastado revoca también el que estaba en uso")
    void laReutilizacionRevocaLaFamiliaEntera() throws Exception {
        Sesion inicial = login("jinbe");
        Sesion renovada = refrescar(inicial.refreshToken());

        // Alguien intenta usar el token viejo: se interpreta como robo.
        mockMvc.perform(refrescoCon(inicial.refreshToken()))
                .andExpect(status().isUnauthorized());

        // El token legítimo en curso también queda invalidado.
        mockMvc.perform(refrescoCon(renovada.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token de refresco inventado devuelve 401")
    void tokenDeRefrescoInventadoDevuelve401() throws Exception {
        mockMvc.perform(refrescoCon("esto-no-lo-ha-emitido-nadie"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El logout invalida el token de refresco")
    void elLogoutInvalidaElTokenDeRefresco() throws Exception {
        Sesion sesion = login("jinbe");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoConToken(sesion.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(refrescoCon(sesion.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cambiar la contraseña exige acertar la actual")
    void cambiarContrasenaExigeLaActual() throws Exception {
        mockMvc.perform(cambioDeContrasena("jinbe", "NoEsLaMia1", "NuevaClave1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La contraseña nueva no puede ser la misma que la actual")
    void laContrasenaNuevaTieneQueSerDistinta() throws Exception {
        mockMvc.perform(cambioDeContrasena("jinbe", TestDataFactory.PASSWORD, TestDataFactory.PASSWORD))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cambiar la contraseña cierra las sesiones abiertas y habilita la nueva clave")
    void cambiarContrasenaCierraLasSesiones() throws Exception {
        Sesion sesion = login("jinbe");

        mockMvc.perform(cambioDeContrasena("jinbe", TestDataFactory.PASSWORD, "NuevaClave1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(refrescoCon(sesion.refreshToken()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "jinbe", "password": "NuevaClave1"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Suspender una cuenta invalida sus tokens de refresco")
    void suspenderInvalidaLosTokens() throws Exception {
        Sesion sesion = login("jinbe");
        data.user("garp", UserRole.ROLE_MODERATOR);

        mockMvc.perform(put("/api/users/jinbe/suspend")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(status().isNoContent());

        mockMvc.perform(refrescoCon(sesion.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Borrar la cuenta invalida sus tokens de refresco")
    void borrarLaCuentaInvalidaLosTokens() throws Exception {
        Sesion sesion = login("jinbe");

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(refrescoCon(sesion.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    private record Sesion(String accessToken, String refreshToken) {
    }

    private Sesion login(String username) throws Exception {
        String cuerpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "%s", "password": "%s"}
                                """.formatted(username, TestDataFactory.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new Sesion(JsonPath.read(cuerpo, "$.accessToken"), JsonPath.read(cuerpo, "$.refreshToken"));
    }

    private Sesion refrescar(String refreshToken) throws Exception {
        String cuerpo = mockMvc.perform(refrescoCon(refreshToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new Sesion(JsonPath.read(cuerpo, "$.accessToken"), JsonPath.read(cuerpo, "$.refreshToken"));
    }

    private org.springframework.test.web.servlet.RequestBuilder refrescoCon(String refreshToken) {
        return post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoConToken(refreshToken));
    }

    private org.springframework.test.web.servlet.RequestBuilder cambioDeContrasena(
            String username, String actual, String nueva) {
        return put("/api/users/me/password")
                .header(HttpHeaders.AUTHORIZATION, data.bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(actual, nueva));
    }

    private String cuerpoConToken(String refreshToken) {
        return """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
    }
}
