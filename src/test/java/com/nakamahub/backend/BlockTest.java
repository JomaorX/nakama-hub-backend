package com.nakamahub.backend;

import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
import com.nakamahub.backend.models.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(TestDataFactory.class)
class BlockTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    private User molesto;
    private User victima;
    private Post postDelMolesto;
    private Post postDeLaVictima;

    @BeforeEach
    void setUp() {
        data.clear();
        molesto = data.user("crocodile");
        victima = data.user("vivi");
        postDelMolesto = data.post(molesto, "Post del molesto", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        postDeLaVictima = data.post(victima, "Post de la víctima", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
    }

    @Test
    @DisplayName("Bloquear oculta las publicaciones del bloqueado en el feed")
    void bloquearOcultaLasPublicaciones() throws Exception {
        mockMvc.perform(get("/api/posts").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(put("/api/users/crocodile/block")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Post de la víctima"));
    }

    @Test
    @DisplayName("El bloqueo funciona en los dos sentidos aunque solo uno lo haya pedido")
    void elBloqueoEsSimetrico() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(get("/api/posts").header(HttpHeaders.AUTHORIZATION, data.bearer("crocodile")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Post del molesto"));
    }

    @Test
    @DisplayName("Un anónimo sigue viendo a los dos: el bloqueo es entre ellos")
    void elAnonimoNoSeVeAfectado() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("El bloqueado no puede abrir el post ni comentar en él")
    void elBloqueadoNoPuedeInteractuar() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(get("/api/posts/" + postDeLaVictima.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("crocodile")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/comments")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("crocodile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\": " + postDeLaVictima.getId()
                                + ", \"content\": \"Vengo a molestar otra vez\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El bloqueado no ve el perfil, pero quien bloqueó sí ve el suyo")
    void elPerfilSoloDesapareceParaElBloqueado() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(get("/api/users/vivi").header(HttpHeaders.AUTHORIZATION, data.bearer("crocodile")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/users/crocodile").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedByMe").value(true));
    }

    @Test
    @DisplayName("Bloquear deshace el seguimiento en ambos sentidos y devuelve la reputación")
    void bloquearDeshaceElSeguimiento() throws Exception {
        data.follow(molesto, victima);
        data.follow(victima, molesto);

        mockMvc.perform(put("/api/users/crocodile/block")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")));

        mockMvc.perform(get("/api/users/crocodile").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(jsonPath("$.followersCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(0));
    }

    @Test
    @DisplayName("No se puede seguir a alguien con quien hay un bloqueo")
    void noSePuedeSeguirTrasElBloqueo() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(put("/api/users/vivi/follow")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("crocodile")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El buscador tampoco devuelve contenido de cuentas bloqueadas")
    void elBuscadorRespetaElBloqueo() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(get("/api/search/posts").param("q", "Post")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Post de la víctima"));
    }

    @Test
    @DisplayName("Los comentarios de una cuenta bloqueada desaparecen del hilo")
    void losComentariosDelBloqueadoDesaparecen() throws Exception {
        data.comment(molesto, postDelMolesto, "Un comentario en mi propio post");
        data.comment(victima, postDelMolesto, "Otro comentario de otra persona");

        mockMvc.perform(get("/api/comments/post/" + postDelMolesto.getId()))
                .andExpect(jsonPath("$.content", hasSize(2)));

        data.block(victima, molesto);

        mockMvc.perform(get("/api/comments/post/" + postDelMolesto.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Volver a pulsar desbloquea y todo vuelve a verse")
    void volverAPulsarDesbloquea() throws Exception {
        mockMvc.perform(put("/api/users/crocodile/block")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")));
        mockMvc.perform(put("/api/users/crocodile/block")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockedByMe").value(false));

        mockMvc.perform(get("/api/posts").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("La lista de bloqueados es privada de cada usuario")
    void laListaDeBloqueadosEsPrivada() throws Exception {
        data.block(victima, molesto);

        mockMvc.perform(get("/api/users/me/blocked").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("crocodile"));

        mockMvc.perform(get("/api/users/me/blocked").header(HttpHeaders.AUTHORIZATION, data.bearer("crocodile")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Nadie puede bloquearse a sí mismo")
    void nadiePuedeBloquearseASiMismo() throws Exception {
        mockMvc.perform(put("/api/users/vivi/block").header(HttpHeaders.AUTHORIZATION, data.bearer("vivi")))
                .andExpect(status().isBadRequest());
    }
}
