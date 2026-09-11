package com.nakamahub.backend;

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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(TestDataFactory.class)
class TimelineTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    private User lector;
    private User seguido;
    private User desconocido;

    @BeforeEach
    void setUp() {
        data.clear();
        lector = data.user("lector");
        seguido = data.user("seguido");
        desconocido = data.user("desconocido");
    }

    @Test
    @DisplayName("Sin seguir a nadie, el timeline solo trae lo publicado por uno mismo")
    void sinSeguidosSoloVeLoSuyo() throws Exception {
        data.post(lector, "Mi primer post", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.post(desconocido, "Post de un desconocido", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Mi primer post"));
    }

    @Test
    @DisplayName("El timeline trae lo de las cuentas seguidas y deja fuera al resto")
    void traeLoDeLosSeguidosYNoLoDemas() throws Exception {
        data.follow(lector, seguido);
        data.post(seguido, "Novedades del capítulo", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.post(desconocido, "Post de un desconocido", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Novedades del capítulo"));
    }

    @Test
    @DisplayName("Un seguidor ve los posts restringidos a seguidores de quien sigue")
    void veLosPostsRestringidosASeguidores() throws Exception {
        data.follow(lector, seguido);
        data.post(seguido, "Solo para los míos", PostStatus.PUBLISHED, PrivacyLevel.FOLLOWERS_ONLY);
        data.post(seguido, "Para todo el mundo", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title",
                        containsInAnyOrder("Solo para los míos", "Para todo el mundo")));
    }

    @Test
    @DisplayName("El timeline no trae borradores ni posts privados de las cuentas seguidas")
    void noTraeBorradoresNiPrivadosDeLosSeguidos() throws Exception {
        data.follow(lector, seguido);
        data.post(seguido, "Borrador ajeno", PostStatus.DRAFT, PrivacyLevel.PUBLIC);
        data.post(seguido, "Privado ajeno", PostStatus.PUBLISHED, PrivacyLevel.PRIVATE);

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("El timeline tampoco trae los borradores ni los privados de uno mismo")
    void noTraeLosBorradoresPropios() throws Exception {
        data.post(lector, "Mi borrador", PostStatus.DRAFT, PrivacyLevel.PUBLIC);
        data.post(lector, "Mi post privado", PostStatus.PUBLISHED, PrivacyLevel.PRIVATE);
        data.post(lector, "Mi post publicado", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Mi post publicado"));
    }

    @Test
    @DisplayName("El timeline exige token, no es una vista pública")
    void elTimelineExigeToken() throws Exception {
        mockMvc.perform(get("/api/posts/feed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Dejar de seguir a una cuenta la saca del timeline")
    void dejarDeSeguirLaSacaDelTimeline() throws Exception {
        data.follow(lector, seguido);
        data.post(seguido, "Novedades del capítulo", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(put("/api/users/seguido/follow")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/feed").header(HttpHeaders.AUTHORIZATION, data.bearer("lector")))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
