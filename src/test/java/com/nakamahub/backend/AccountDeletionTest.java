package com.nakamahub.backend;

import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.UserRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(TestDataFactory.class)
class AccountDeletionTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    @Autowired
    UserRepository userRepository;

    private User nami;
    private User usopp;

    @BeforeEach
    void setUp() {
        data.clear();
        nami = data.user("nami");
        usopp = data.user("usopp");
    }

    @Test
    @DisplayName("Se puede borrar una cuenta que ha comentado en posts de otros")
    void borrarCuentaConComentariosAjenosNoRompe() throws Exception {
        Post postDeUsopp = data.post(usopp, "Mi teoría", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.comment(nami, postDeUsopp, "No estoy de acuerdo con esta teoría");

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isNoContent());

        // El comentario sobrevive, atribuido ya a la cuenta anonimizada.
        mockMvc.perform(get("/api/comments/post/" + postDeUsopp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].authorUsername")
                        .value("usuario_eliminado_" + nami.getId()));
    }

    @Test
    @DisplayName("La cuenta borrada queda anonimizada y marcada como DELETED")
    void cuentaBorradaQuedaAnonimizada() throws Exception {
        mockMvc.perform(put("/api/users/me/bio")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("nami"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bio": "Navegante de los Mugiwara"}
                        """));

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isNoContent());

        User borrada = userRepository.findById(nami.getId()).orElseThrow();
        assertThat(borrada.getStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(borrada.getUsername()).isEqualTo("usuario_eliminado_" + nami.getId());
        assertThat(borrada.getEmail()).doesNotContain("nami@");
        assertThat(borrada.getBio()).isNull();
        assertThat(borrada.getAvatarUrl()).isNull();
        assertThat(borrada.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Tras el borrado el token antiguo deja de servir y el login falla")
    void cuentaBorradaNoPuedeVolverAEntrar() throws Exception {
        String token = data.bearer("nami");

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier": "nami", "password": "%s"}
                                """.formatted(TestDataFactory.PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El perfil de una cuenta borrada no se puede consultar")
    void perfilDeCuentaBorradaDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/nami"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El email queda liberado para un registro nuevo")
    void elEmailQuedaLibreTrasElBorrado() throws Exception {
        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nami@nakamahub.test", "username": "namiNueva", "password": "Secreta1"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("El borrado deshace los seguimientos y devuelve la reputación concedida")
    void borrarCuentaDeshaceSeguimientos() throws Exception {
        data.follow(nami, usopp);
        assertThat(userRepository.findById(usopp.getId()).orElseThrow().getReputationPoints()).isEqualTo(1);

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isNoContent());

        User usoppTrasElBorrado = userRepository.findById(usopp.getId()).orElseThrow();
        assertThat(usoppTrasElBorrado.getReputationPoints()).isZero();

        mockMvc.perform(get("/api/users/usopp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followersCount").value(0));
    }

    @Test
    @DisplayName("El borrado retira los likes y descuenta el contador del post")
    void borrarCuentaRetiraLosLikes() throws Exception {
        Post postDeUsopp = data.post(usopp, "Mi teoría", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.like(nami, postDeUsopp);

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + postDeUsopp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(0));
    }

    @Test
    @DisplayName("Nadie puede registrarse con el prefijo reservado de cuentas borradas")
    void elPrefijoDeCuentasBorradasEstaReservado() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "impostor@nakamahub.test", "username": "usuario_eliminado_1", "password": "Secreta1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un moderador borra una cuenta con el mismo borrado lógico")
    void moderadorBorraCuenta() throws Exception {
        data.user("garp", UserRole.ROLE_MODERATOR);

        mockMvc.perform(delete("/api/users/nami")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(nami.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountStatus.DELETED);
    }

    @Test
    @DisplayName("Borrar dos veces la misma cuenta devuelve 409")
    void borrarDosVecesDevuelveConflicto() throws Exception {
        data.user("garp", UserRole.ROLE_MODERATOR);
        String moderador = data.bearer("garp");

        mockMvc.perform(delete("/api/users/nami").header(HttpHeaders.AUTHORIZATION, moderador))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/usuario_eliminado_" + nami.getId())
                        .header(HttpHeaders.AUTHORIZATION, moderador))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("La exportación de datos devuelve el perfil, los posts y los comentarios")
    void exportacionDeDatosDevuelveTodo() throws Exception {
        Post postDeNami = data.post(nami, "Mapa del Grand Line", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.comment(nami, postDeNami, "Un comentario sobre mi propio post");
        data.follow(usopp, nami);

        mockMvc.perform(get("/api/users/me/export").header(HttpHeaders.AUTHORIZATION, data.bearer("nami")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.username").value("nami"))
                .andExpect(jsonPath("$.account.email").value("nami@nakamahub.test"))
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.comments", hasSize(1)))
                .andExpect(jsonPath("$.followers", hasSize(1)))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }
}
