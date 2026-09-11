package com.nakamahub.backend;

import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.models.UserRole;
import com.nakamahub.backend.support.IntegrationTest;
import com.nakamahub.backend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(TestDataFactory.class)
class PostVisibilityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    private User autor;
    private Post publicado;
    private Post borrador;
    private Post privado;
    private Post soloSeguidores;

    @BeforeEach
    void setUp() {
        data.clear();
        autor = data.user("shanks");
        publicado = data.post(autor, "Publicado", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        borrador = data.post(autor, "Borrador", PostStatus.DRAFT, PrivacyLevel.PUBLIC);
        privado = data.post(autor, "Privado", PostStatus.PUBLISHED, PrivacyLevel.PRIVATE);
        soloSeguidores = data.post(autor, "Seguidores", PostStatus.PUBLISHED, PrivacyLevel.FOLLOWERS_ONLY);
    }

    @Test
    @DisplayName("Un visitante anónimo solo ve los posts publicados y públicos")
    void anonimoSoloVeLoPublico() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[*].title", contains("Publicado")));
    }

    @Test
    @DisplayName("El autor ve sus propios borradores y privados en el feed")
    void autorVeSusPropiosPosts() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("shanks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)));
    }

    @Test
    @DisplayName("Un usuario ajeno no ve los borradores ni los privados de otro")
    void otroUsuarioNoVeBorradoresAjenos() throws Exception {
        data.user("mihawk");

        mockMvc.perform(get("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("mihawk")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[*].title", contains("Publicado")));
    }

    @Test
    @DisplayName("Un seguidor ve además los posts restringidos a seguidores")
    void seguidorVePostsDeSeguidores() throws Exception {
        User seguidor = data.user("yasopp");
        data.follow(seguidor, autor);

        mockMvc.perform(get("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("yasopp")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("El feed dice si el visitante ya dio me gusta a cada post")
    void elFeedIndicaSiYaSeDioMeGusta() throws Exception {
        User lector = data.user("mihawk");
        data.like(lector, publicado);

        mockMvc.perform(get("/api/posts").header(HttpHeaders.AUTHORIZATION, data.bearer("mihawk")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likedByMe").value(true))
                .andExpect(jsonPath("$.content[0].own").value(false));

        mockMvc.perform(get("/api/posts"))
                .andExpect(jsonPath("$.content[0].likedByMe").value(false));
    }

    @Test
    @DisplayName("El autor recibe sus posts marcados como propios")
    void elAutorRecibeSusPostsComoPropios() throws Exception {
        mockMvc.perform(get("/api/posts/" + publicado.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("shanks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.own").value(true));
    }

    @Test
    @DisplayName("El perfil dice si el visitante ya sigue a esa cuenta")
    void elPerfilIndicaSiYaSeSigue() throws Exception {
        User seguidor = data.user("yasopp");
        data.follow(seguidor, autor);

        mockMvc.perform(get("/api/users/shanks").header(HttpHeaders.AUTHORIZATION, data.bearer("yasopp")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedByMe").value(true))
                .andExpect(jsonPath("$.own").value(false));

        mockMvc.perform(get("/api/users/shanks").header(HttpHeaders.AUTHORIZATION, data.bearer("shanks")))
                .andExpect(jsonPath("$.own").value(true))
                .andExpect(jsonPath("$.followedByMe").value(false));

        mockMvc.perform(get("/api/users/shanks"))
                .andExpect(jsonPath("$.followedByMe").value(false));
    }

    @Test
    @DisplayName("Pedir un post privado por id devuelve 404, sin confirmar que existe")
    void postPrivadoPorIdDevuelve404() throws Exception {
        mockMvc.perform(get("/api/posts/" + privado.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/posts/" + borrador.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/posts/" + soloSeguidores.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un post público sí se sirve a un anónimo y suma una visita")
    void postPublicoSeSirveYSumaVisita() throws Exception {
        mockMvc.perform(get("/api/posts/" + publicado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Publicado"))
                .andExpect(jsonPath("$.viewsCount").value(1));
    }

    @Test
    @DisplayName("Los comentarios de un post privado no se pueden listar")
    void comentariosDePostPrivadoNoSeListan() throws Exception {
        mockMvc.perform(get("/api/comments/post/" + privado.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El perfil público de un usuario es accesible sin token y oculta sus borradores")
    void perfilPublicoAccesibleSinToken() throws Exception {
        mockMvc.perform(get("/api/users/shanks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("shanks"))
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.postsCount").value(1));
    }
}
